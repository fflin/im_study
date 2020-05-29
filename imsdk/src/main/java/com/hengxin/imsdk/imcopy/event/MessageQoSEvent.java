package com.hengxin.imsdk.imcopy.event;

import com.hengxin.imsdk.server.Protocal;

import java.util.ArrayList;

/**
 * author : fflin
 * date   : 2020/5/29 16:01
 * desc   : 消息质量控制
 * version: 1.0
 */
public interface MessageQoSEvent {
    void messagesLost(ArrayList<Protocal> var1);

    void messagesBeReceived(String var1);
}