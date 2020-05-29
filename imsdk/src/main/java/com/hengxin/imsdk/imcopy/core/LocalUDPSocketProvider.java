package com.hengxin.imsdk.imcopy.core;

import android.util.Log;

import com.hengxin.imsdk.imcopy.ClientCoreSDK;
import com.hengxin.imsdk.imcopy.config.ConfigEntity;

import java.net.DatagramSocket;

/**
 * author : fflin
 * date   : 2020/5/29 16:02
 * desc   : 本地UDP连接侦听的管理类 负责开关 重置连接
 * version: 1.0
 */
public class LocalUDPSocketProvider {
    private static final String TAG = LocalUDPSocketProvider.class.getSimpleName();
    private static LocalUDPSocketProvider instance = null;
    private DatagramSocket localUDPSocket = null;

    public static LocalUDPSocketProvider getInstance() {
        if (instance == null) {
            instance = new LocalUDPSocketProvider();
        }

        return instance;
    }

    private LocalUDPSocketProvider() {
    }

    public DatagramSocket resetLocalUDPSocket() {
        try {
            this.closeLocalUDPSocket();
            this.localUDPSocket = ConfigEntity.localUDPPort == 0 ? new DatagramSocket() : new DatagramSocket(ConfigEntity.localUDPPort);
            this.localUDPSocket.setReuseAddress(true);
            return this.localUDPSocket;
        } catch (Exception var2) {
            Log.w(TAG, "【IMCORE】localUDPSocket创建时出错，原因是：" + var2.getMessage(), var2);
            this.closeLocalUDPSocket();
            return null;
        }
    }

    private boolean isLocalUDPSocketReady() {
        return this.localUDPSocket != null && !this.localUDPSocket.isClosed();
    }

    public DatagramSocket getLocalUDPSocket() {
        return this.isLocalUDPSocketReady() ? this.localUDPSocket : this.resetLocalUDPSocket();
    }

    public void closeLocalUDPSocket() {
        this.closeLocalUDPSocket(true);
    }

    public void closeLocalUDPSocket(boolean silent) {
        try {
            if (ClientCoreSDK.DEBUG && !silent) {
                Log.d(TAG, "【IMCORE】正在closeLocalUDPSocket()...");
            }

            if (this.localUDPSocket != null) {
                this.localUDPSocket.close();
                this.localUDPSocket = null;
            } else if (!silent) {
                Log.d(TAG, "【IMCORE】Socket处于未初化状态（可能是您还未登陆），无需关闭。");
            }
        } catch (Exception var3) {
            if (!silent) {
                Log.w(TAG, "【IMCORE】lcloseLocalUDPSocket时出错，原因是：" + var3.getMessage(), var3);
            }
        }

    }
}
