package com.hengxin.imsdk.imcopy.config;

import com.hengxin.imsdk.imcopy.core.KeepAliveDaemon;

/**
 * author : fflin
 * date   : 2020/5/29 16:03
 * desc   : 配置信息
 * version: 1.0
 */
public class ConfigEntity {
    public static String appKey = null;
    public static String serverIP = "rbcore.52im.net";
    public static int serverUDPPort = 7901;
    public static int localUDPPort = 0;

    public ConfigEntity() {
    }

    public static void setSenseMode(SenseMode mode) {
        int keepAliveInterval = 0;
        int networkConnectionTimeout = 0;
        switch (mode) {
            case MODE_3S:
                keepAliveInterval = 3000;
                networkConnectionTimeout = 10000;
                break;
            case MODE_10S:
                keepAliveInterval = 10000;
                networkConnectionTimeout = 21000;
                break;
            case MODE_30S:
                keepAliveInterval = 30000;
                networkConnectionTimeout = 61000;
                break;
            case MODE_60S:
                keepAliveInterval = 60000;
                networkConnectionTimeout = 121000;
                break;
            case MODE_120S:
                keepAliveInterval = 120000;
                networkConnectionTimeout = 241000;
        }

        if (keepAliveInterval > 0) {
            KeepAliveDaemon.KEEP_ALIVE_INTERVAL = keepAliveInterval;
        }

        if (networkConnectionTimeout > 0) {
            KeepAliveDaemon.NETWORK_CONNECTION_TIME_OUT = networkConnectionTimeout;
        }

    }

    public static enum SenseMode {
        MODE_3S,
        MODE_10S,
        MODE_30S,
        MODE_60S,
        MODE_120S;

        private SenseMode() {
        }
    }
}