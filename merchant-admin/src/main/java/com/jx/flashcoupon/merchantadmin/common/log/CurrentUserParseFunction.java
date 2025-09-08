package com.jx.flashcoupon.merchantadmin.common.log;

import com.jx.flashcoupon.merchantadmin.common.context.UserContext;
import com.mzt.logapi.service.IParseFunction;
import org.springframework.stereotype.Component;

/**
 * 操作日志组件解析当前登录用户信息
 */
@Component
public class CurrentUserParseFunction implements IParseFunction {

    @Override
    public String functionName() {
        return "CURRENT_USER";
    }

    @Override
    public String apply(Object value) {
        return UserContext.getUsername();
    }
}