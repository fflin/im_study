package com.hengxin.imsdk.imcopy.event;

/**
 * author : fflin
 * date   : 2020/5/29 16:01
 * desc   : 消息转化？
 * version: 1.0
 */
public interface ChatTransDataEvent {
    void onTransBuffer(String var1, String var2, String var3, int var4);

    void onErrorResponse(int var1, String var2);
}
