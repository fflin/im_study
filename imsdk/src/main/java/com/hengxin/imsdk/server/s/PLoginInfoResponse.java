package com.hengxin.imsdk.server.s;

/**
 * author : fflin
 * date   : 2020/5/27 20:23
 * desc   :
 * version: 1.0
 */
public class PLoginInfoResponse {
    private int code = 0;

    public PLoginInfoResponse(int code) {
        this.code = code;
    }

    public int getCode() {
        return this.code;
    }

    public void setCode(int code) {
        this.code = code;
    }
}
