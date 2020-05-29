package com.hengxin.imsdk.server.s;

/**
 * author : fflin
 * date   : 2020/5/27 20:23
 * desc   :
 * version: 1.0
 */
public class PErrorResponse {
    private int errorCode = -1;
    private String errorMsg = null;

    public PErrorResponse(int errorCode, String errorMsg) {
        this.errorCode = errorCode;
        this.errorMsg = errorMsg;
    }

    public int getErrorCode() {
        return this.errorCode;
    }

    public void setErrorCode(int errorCode) {
        this.errorCode = errorCode;
    }

    public String getErrorMsg() {
        return this.errorMsg;
    }

    public void setErrorMsg(String errorMsg) {
        this.errorMsg = errorMsg;
    }
}
