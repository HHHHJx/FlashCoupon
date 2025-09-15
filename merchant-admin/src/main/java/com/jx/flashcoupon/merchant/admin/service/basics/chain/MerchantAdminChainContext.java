

package com.jx.flashcoupon.merchant.admin.service.basics.chain;

import com.jx.flashcoupon.merchant.admin.service.handler.filter.CouponTemplateCreateParamBaseVerifyChainFilter;
import com.jx.flashcoupon.merchant.admin.service.handler.filter.CouponTemplateCreateParamNotNullChainFilter;
import com.jx.flashcoupon.merchant.admin.service.handler.filter.CouponTemplateCreateParamVerifyChainFilter;
import org.springframework.beans.BeansException;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;
import org.springframework.core.Ordered;
import org.springframework.stereotype.Component;
import org.springframework.util.CollectionUtils;

import java.util.*;

/**
 * 商家后管责任链上下文容器
 * ApplicationContextAware 接口获取应用上下文，并复制局部变量方便后续使用；
 * CommandLineRunner 项目启动后执行责任链容器的填充工作：实现了 CommandLineRunner 的 run(...) 方法，会在 Spring Boot 启动完成、IOC 初始化后由框架自动执行一次。
 */
@Component
public final class MerchantAdminChainContext<T> implements ApplicationContextAware, CommandLineRunner {

    /**
     * 应用上下文，我们这里通过 Spring IOC 获取 Bean 实例
     */
    private ApplicationContext applicationContext;

    /**
     * 保存商家后管责任链实现类
     */
    private final Map<String, List<MerchantAdminAbstractChainHandler>> abstractChainHandlerContainer = new HashMap<>();

    /**
     * “按标识取链并执行”（消费容器数据）
     * @param mark         责任链组件标识
     * @param requestParam 请求参数
     */
    public void handler(String mark, T requestParam) {
        List<MerchantAdminAbstractChainHandler> abstractChainHandlers = abstractChainHandlerContainer.get(mark);
        if (CollectionUtils.isEmpty(abstractChainHandlers)) {
            throw new RuntimeException(String.format("[%s] Chain of Responsibility ID is undefined.", mark));
        }
        abstractChainHandlers.forEach(each -> each.handler(requestParam));
    }

    /**
     * “建链+排序”（初始化容器数据）
     */
    @Override
    public void run(String... args) throws Exception {
        // 从 Spring IOC 容器中获取指定接口 Spring Bean 集合
        Map<String, MerchantAdminAbstractChainHandler> chainFilterMap = applicationContext.getBeansOfType(MerchantAdminAbstractChainHandler.class);
        chainFilterMap.forEach((beanName, bean) -> {
            // 判断 Mark 是否已经存在抽象责任链容器中，如果已经存在直接向集合新增；如果不存在，创建 Mark 和对应的集合
            List<MerchantAdminAbstractChainHandler> abstractChainHandlers = abstractChainHandlerContainer.getOrDefault(bean.mark(), new ArrayList<>());
            abstractChainHandlers.add(bean);
            abstractChainHandlerContainer.put(bean.mark(), abstractChainHandlers);
        });
        abstractChainHandlerContainer.forEach((mark, unsortedChainHandlers) -> {
            // 对每个 Mark 对应的责任链实现类集合进行排序，优先级小的在前
            unsortedChainHandlers.sort(Comparator.comparing(Ordered::getOrder));
        });
    }

    @Override
    public void setApplicationContext(ApplicationContext applicationContext) throws BeansException {
        this.applicationContext = applicationContext;
    }
}
