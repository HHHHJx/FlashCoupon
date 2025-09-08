package com.jx.flashcoupon.merchantadmin.service.impl;

import cn.hutool.core.bean.BeanUtil;
import cn.hutool.core.util.IdUtil;
import com.alibaba.excel.EasyExcel;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.jx.flashcoupon.framework.exception.ClientException;
import com.jx.flashcoupon.merchantadmin.common.context.UserContext;
import com.jx.flashcoupon.merchantadmin.common.enums.CouponTaskSendTypeEnum;
import com.jx.flashcoupon.merchantadmin.common.enums.CouponTaskStatusEnum;
import com.jx.flashcoupon.merchantadmin.dao.entity.CouponTaskDO;
import com.jx.flashcoupon.merchantadmin.dao.mapper.CouponTaskMapper;
import com.jx.flashcoupon.merchantadmin.dto.req.CouponTaskCreateReqDTO;
import com.jx.flashcoupon.merchantadmin.dto.req.CouponTaskPageQueryReqDTO;
import com.jx.flashcoupon.merchantadmin.dto.resp.CouponTaskPageQueryRespDTO;
import com.jx.flashcoupon.merchantadmin.dto.resp.CouponTaskQueryRespDTO;
import com.jx.flashcoupon.merchantadmin.dto.resp.CouponTemplateQueryRespDTO;
import com.jx.flashcoupon.merchantadmin.mq.event.CouponTaskExecuteEvent;
import com.jx.flashcoupon.merchantadmin.mq.producer.CouponTaskActualExecuteProducer;
import com.jx.flashcoupon.merchantadmin.service.handler.excel.RowCountListener;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import org.redisson.api.RBlockingDeque;
import org.redisson.api.RDelayedQueue;
import org.redisson.api.RedissonClient;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Objects;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.SynchronousQueue;

/**
 * 优惠券推送业务逻辑实现层
 */
@Service
@RequiredArgsConstructor
public class CouponTaskServiceImpl extends ServiceImpl<CouponTaskMapper, CouponTaskDO> implements CouponTaskService {

    private final CouponTemplateService couponTemplateService;
    private final CouponTaskMapper couponTaskMapper;
    private final RedissonClient redissonClient;
    private final CouponTaskActualExecuteProducer couponTaskActualExecuteProducer;

    /**
     * 为什么这里拒绝策略使用直接丢弃任务？因为在发送任务时如果遇到发送数量为空，会重新进行统计
     */
    private final ExecutorService executorService = new ThreadPoolExecutor(
            Runtime.getRuntime().availableProcessors(),
            Runtime.getRuntime().availableProcessors() << 1,
            60,
            TimeUnit.SECONDS,
            new SynchronousQueue<>(),
            new ThreadPoolExecutor.DiscardPolicy()
    );

    @Transactional(rollbackFor = Exception.class)
    @Override
    public void createCouponTask(CouponTaskCreateReqDTO requestParam) {
        // 验证非空参数
        // 验证参数是否正确，比如文件地址是否为我们期望的格式等
        // 验证参数依赖关系，比如选择定时发送，发送时间是否不为空等
        CouponTemplateQueryRespDTO couponTemplate = couponTemplateService.findCouponTemplateById(requestParam.getCouponTemplateId());
        if (couponTemplate == null) {
            throw new ClientException("优惠券模板不存在，请检查提交信息是否正确");
        }
        // ......

        // 构建优惠券推送任务数据库持久层实体
        CouponTaskDO couponTaskDO = BeanUtil.copyProperties(requestParam, CouponTaskDO.class);
        couponTaskDO.setBatchId(IdUtil.getSnowflakeNextId());
        couponTaskDO.setOperatorId(Long.parseLong(UserContext.getUserId()));
        couponTaskDO.setShopNumber(UserContext.getShopNumber());
        couponTaskDO.setStatus(
                Objects.equals(requestParam.getSendType(), CouponTaskSendTypeEnum.IMMEDIATE.getType())
                        ? CouponTaskStatusEnum.IN_PROGRESS.getStatus()
                        : CouponTaskStatusEnum.PENDING.getStatus()
        );

        // 保存优惠券推送任务记录到数据库
        couponTaskMapper.insert(couponTaskDO);

        // 为什么需要统计行数？因为发送后需要比对所有优惠券是否都已发放到用户账号
        // 100 万数据大概需要 4 秒才能返回前端，如果加上验证将会时间更长，所以这里将最耗时的统计操作异步化
        JSONObject delayJsonObject = JSONObject
                .of("fileAddress", requestParam.getFileAddress(), "couponTaskId", couponTaskDO.getId());
        executorService.execute(() -> refreshCouponTaskSendNum(delayJsonObject));

        // 假设刚把消息提交到线程池，突然应用宕机了，我们通过延迟队列进行兜底 Refresh
        RBlockingDeque<Object> blockingDeque = redissonClient.getBlockingDeque("COUPON_TASK_SEND_NUM_DELAY_QUEUE");
        RDelayedQueue<Object> delayedQueue = redissonClient.getDelayedQueue(blockingDeque);
        // 这里延迟时间设置 20 秒，原因是我们笃定上面线程池 20 秒之内就能结束任务
        delayedQueue.offer(delayJsonObject, 20, TimeUnit.SECONDS);

        // 如果是立即发送任务，直接调用消息队列进行发送流程
        if (Objects.equals(requestParam.getSendType(), CouponTaskSendTypeEnum.IMMEDIATE.getType())) {
            // 执行优惠券推送业务，正式向用户发放优惠券
            CouponTaskExecuteEvent couponTaskExecuteEvent = CouponTaskExecuteEvent.builder()
                    .couponTaskId(couponTaskDO.getId())
                    .build();
            couponTaskActualExecuteProducer.sendMessage(couponTaskExecuteEvent);
        }
    }

    @Override
    public IPage<CouponTaskPageQueryRespDTO> pageQueryCouponTask(CouponTaskPageQueryReqDTO requestParam) {
        // 构建分页查询模板 LambdaQueryWrapper
        LambdaQueryWrapper<CouponTaskDO> queryWrapper = Wrappers.lambdaQuery(CouponTaskDO.class)
                .eq(CouponTaskDO::getShopNumber, UserContext.getShopNumber())
                .eq(StrUtil.isNotBlank(requestParam.getBatchId()), CouponTaskDO::getBatchId, requestParam.getBatchId())
                .like(StrUtil.isNotBlank(requestParam.getTaskName()), CouponTaskDO::getTaskName, requestParam.getTaskName())
                .eq(StrUtil.isNotBlank(requestParam.getCouponTemplateId()), CouponTaskDO::getCouponTemplateId, requestParam.getCouponTemplateId())
                .eq(Objects.nonNull(requestParam.getStatus()), CouponTaskDO::getStatus, requestParam.getStatus());

        // MyBatis-Plus 分页查询优惠券推送任务信息
        IPage<CouponTaskDO> selectPage = couponTaskMapper.selectPage(requestParam, queryWrapper);

        // 转换数据库持久层对象为优惠券模板返回参数
        return selectPage.convert(each -> BeanUtil.toBean(each, CouponTaskPageQueryRespDTO.class));
    }

    @Override
    public CouponTaskQueryRespDTO findCouponTaskById(String taskId) {
        CouponTaskDO couponTaskDO = couponTaskMapper.selectById(taskId);
        return BeanUtil.toBean(couponTaskDO, CouponTaskQueryRespDTO.class);
    }

    private void refreshCouponTaskSendNum(JSONObject delayJsonObject) {
        // 通过 EasyExcel 监听器获取 Excel 中所有行数
        RowCountListener listener = new RowCountListener();
        EasyExcel.read(delayJsonObject.getString("fileAddress"), listener).sheet().doRead();
        int totalRows = listener.getRowCount();

        // 刷新优惠券推送记录中发送行数
        CouponTaskDO updateCouponTaskDO = CouponTaskDO.builder()
                .id(delayJsonObject.getLong("couponTaskId"))
                .sendNum(totalRows)
                .build();
        couponTaskMapper.updateById(updateCouponTaskDO);
    }

    /**
     * 因为静态内部类的 Bean 注入有问题，所以我们这里直接 new 对象运行即可
     * 如果按照上一版本的方式写，refreshCouponTaskSendNum 方法中 couponTaskMapper 为空
     */
    @PostConstruct
    public void init() {
        new RefreshCouponTaskDelayQueueRunner(this, couponTaskMapper, redissonClient).run();
    }

    /**
     * 优惠券延迟刷新发送条数兜底消费者｜这是兜底策略，一般来说不会执行这段逻辑
     * 如果延迟消息没有持久化成功，或者 Redis 挂了怎么办？后续可以人工处理
     * <p>
     * 作者：马丁
     * 加项目群：早加入就是优势！500人内部项目群，分享的知识总有你需要的 <a href="https://t.zsxq.com/cw7b9" />
     * 开发时间：2024-07-12
     */
    @RequiredArgsConstructor
    static class RefreshCouponTaskDelayQueueRunner {

        private final CouponTaskServiceImpl couponTaskService;
        private final CouponTaskMapper couponTaskMapper;
        private final RedissonClient redissonClient;

        public void run() {
            Executors.newSingleThreadExecutor(
                            runnable -> {
                                Thread thread = new Thread(runnable);
                                thread.setName("delay_coupon-task_send-num_consumer");
                                thread.setDaemon(Boolean.TRUE);
                                return thread;
                            })
                    .execute(() -> {
                        RBlockingDeque<JSONObject> blockingDeque = redissonClient.getBlockingDeque("COUPON_TASK_SEND_NUM_DELAY_QUEUE");
                        for (; ; ) {
                            try {
                                // 获取延迟队列已到达时间元素
                                JSONObject delayJsonObject = blockingDeque.take();
                                if (delayJsonObject != null) {
                                    // 获取优惠券推送记录，查看发送条数是否已经有值，有的话代表上面线程池已经处理完成，无需再处理
                                    CouponTaskDO couponTaskDO = couponTaskMapper.selectById(delayJsonObject.getLong("couponTaskId"));
                                    if (couponTaskDO.getSendNum() == null) {
                                        couponTaskService.refreshCouponTaskSendNum(delayJsonObject);
                                    }
                                }
                            } catch (Throwable ignored) {
                            }
                        }
                    });
        }
    }
}