

package com.jx.flashcoupon.merchant.admin.service.basics.chain;

import org.springframework.core.Ordered;

/**
 * 商家后管业务责任链组件的抽象接口
 */
public interface MerchantAdminAbstractChainHandler<T> extends Ordered {

    /**
     * 执行责任链逻辑
     *
     * @param requestParam 责任链执行入参
     */
    void handler(T requestParam);

    /**
     * @return 责任链组件标识
     */
    String mark();
}
