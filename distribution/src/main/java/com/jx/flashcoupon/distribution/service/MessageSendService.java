

package com.jx.flashcoupon.distribution.service;

import com.nageoffer.onecoupon.distribution.dto.req.MessageSendReqDTO;
import com.nageoffer.onecoupon.distribution.dto.resp.MessageSendRespDTO;

/**
 * 消息发送接口
 * 正常来说这应该有个独立消息服务，因为消息通知不在牛券系统核心范畴，所以仅展示流程
 * <p>
 * 作者：马丁
 * 加项目群：早加入就是优势！500人内部沟通群，分享的知识总有你需要的 <a href="https://t.zsxq.com/cw7b9" />
 * 开发时间：2024-07-16
 */
public interface MessageSendService {

    /**
     * 消息发送接口
     *
     * @param requestParam 消息发送请求参数
     * @return 消息发送结果
     */
    MessageSendRespDTO sendMessage(MessageSendReqDTO requestParam);
}
