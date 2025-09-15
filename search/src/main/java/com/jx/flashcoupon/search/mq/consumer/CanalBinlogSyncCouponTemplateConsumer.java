

package com.jx.flashcoupon.search.mq.consumer;

import cn.hutool.core.bean.BeanUtil;
import cn.hutool.core.collection.CollUtil;
import com.jx.flashcoupon.search.common.constant.SearchRockerMQConstant;
import com.jx.flashcoupon.search.common.enums.CouponTemplateStatusEnum;
import com.jx.flashcoupon.search.dao.entity.CouponTemplateDO;
import com.jx.flashcoupon.search.dao.entity.CouponTemplateDoc;
import com.jx.flashcoupon.search.mq.event.CanalBinlogEvent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.rocketmq.spring.annotation.RocketMQMessageListener;
import org.apache.rocketmq.spring.core.RocketMQListener;
import org.springframework.data.elasticsearch.client.elc.ElasticsearchTemplate;
import org.springframework.stereotype.Component;

import java.util.Map;

/**
 * 通过 Canal 监听优惠券模板表 Binlog 投递消息队列消费
 */
@Slf4j
@Component
@RequiredArgsConstructor
@RocketMQMessageListener(
        topic = SearchRockerMQConstant.TEMPLATE_COUPON_BINLOG_SYNC_TOPIC_KEY,
        consumerGroup = SearchRockerMQConstant.TEMPLATE_COUPON_BINLOG_SYNC_CG_KEY
)
public class CanalBinlogSyncCouponTemplateConsumer implements RocketMQListener<CanalBinlogEvent> {

    private final ElasticsearchTemplate elasticsearchTemplate;

    @Override
    public void onMessage(CanalBinlogEvent canalBinlogEvent) {
        log.info("{}", canalBinlogEvent);
        Map<String, Object> first = CollUtil.getFirst(canalBinlogEvent.getData());
        CouponTemplateDO couponTemplate = BeanUtil.toBean(first, CouponTemplateDO.class);
        CouponTemplateDoc couponTemplateDoc = BeanUtil.copyProperties(couponTemplate, CouponTemplateDoc.class);
        switch (canalBinlogEvent.getType()) {
            // 新增优惠券
            case "INSERT": {
                elasticsearchTemplate.save(couponTemplateDoc);
                break;
            }
            // 更新优惠券
            case "UPDATE": {
                // 优惠券已结束或已删除, 都将该数据从ES中移除, 否则进行更新操作
                if (CouponTemplateStatusEnum.EXPIRED.getCode().equals(couponTemplate.getStatus())
                        || CouponTemplateStatusEnum.DELETED.getCode().equals(couponTemplate.getDelFlag())) {
                    elasticsearchTemplate.delete(couponTemplateDoc);
                    break;
                }
                boolean exists = elasticsearchTemplate.exists(couponTemplateDoc.getId().toString(), CouponTemplateDoc.class);
                // 当更新主体不存在时，则转换成新增
                if (!exists) {
                    elasticsearchTemplate.save(couponTemplateDoc);
                    break;
                }
                elasticsearchTemplate.update(couponTemplateDoc);
                break;
            }
            // 硬删除优惠券, 一般业务上不会走该分支, 为了避免直接操作数据库导致数据不一致, 因此也加上该段逻辑
            case "DELETE": {
                elasticsearchTemplate.delete(couponTemplateDoc);
                break;
            }
            default:
        }
    }
}
