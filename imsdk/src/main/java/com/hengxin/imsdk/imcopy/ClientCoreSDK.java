package com.hengxin.imsdk.imcopy;

import android.app.Application;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.util.Log;

import com.hengxin.imsdk.imcopy.core.AutoReLoginDaemon;
import com.hengxin.imsdk.imcopy.core.KeepAliveDaemon;
import com.hengxin.imsdk.imcopy.core.LocalUDPDataReciever;
import com.hengxin.imsdk.imcopy.core.LocalUDPSocketProvider;
import com.hengxin.imsdk.imcopy.core.QoS4ReciveDaemon;
import com.hengxin.imsdk.imcopy.core.QoS4SendDaemon;
import com.hengxin.imsdk.imcopy.event.ChatBaseEvent;
import com.hengxin.imsdk.imcopy.event.ChatTransDataEvent;
import com.hengxin.imsdk.imcopy.event.MessageQoSEvent;

/**
 * author : fflin
 * date   : 2020/5/27 20:16
 * desc   :
 * version: 1.0
 */
public class ClientCoreSDK {
    private static final String TAG = ClientCoreSDK.class.getSimpleName();
    public static boolean DEBUG = true;
    public static boolean autoReLogin = true;
    private static ClientCoreSDK instance = null;
    private boolean _init = false;
    private boolean localDeviceNetworkOk = true;
    private boolean connectedToServer = true;
    private boolean loginHasInit = false;
    private String currentLoginUserId = null;
    private String currentLoginToken = null;
    private String currentLoginExtra = null;
    private ChatBaseEvent chatBaseEvent = null;
    private ChatTransDataEvent chatTransDataEvent = null;
    private MessageQoSEvent messageQoSEvent = null;
    private Context context = null;
    private final BroadcastReceiver networkConnectionStatusBroadcastReceiver = new BroadcastReceiver() {
        public void onReceive(Context context, Intent intent) {
            ConnectivityManager connectMgr = (ConnectivityManager)context.getSystemService(Context.CONNECTIVITY_SERVICE);
            NetworkInfo mobNetInfo = connectMgr.getNetworkInfo(0);
            NetworkInfo wifiNetInfo = connectMgr.getNetworkInfo(1);
            NetworkInfo ethernetInfo = connectMgr.getNetworkInfo(9);
            if ((mobNetInfo == null || !mobNetInfo.isConnected()) && (wifiNetInfo == null || !wifiNetInfo.isConnected()) && (ethernetInfo == null || !ethernetInfo.isConnected())) {
                Log.e(ClientCoreSDK.TAG, "【IMCORE】【本地网络通知】检测本地网络连接断开了!");
                ClientCoreSDK.this.localDeviceNetworkOk = false;
                LocalUDPSocketProvider.getInstance().closeLocalUDPSocket();
            } else {
                if (ClientCoreSDK.DEBUG) {
                    Log.e(ClientCoreSDK.TAG, "【IMCORE】【本地网络通知】检测本地网络已连接上了!");
                }

                ClientCoreSDK.this.localDeviceNetworkOk = true;
                LocalUDPSocketProvider.getInstance().closeLocalUDPSocket();
            }

        }
    };

    public static ClientCoreSDK getInstance() {
        if (instance == null) {
            instance = new ClientCoreSDK();
        }

        return instance;
    }

    private ClientCoreSDK() {
    }

    public void init(Context _context) {
        if (!this._init) {
            if (_context == null) {
                throw new IllegalArgumentException("context can't be null!");
            }

            if (_context instanceof Application) {
                this.context = _context;
            } else {
                this.context = _context.getApplicationContext();
            }

            IntentFilter intentFilter = new IntentFilter();
            intentFilter.addAction("android.net.conn.CONNECTIVITY_CHANGE");
            this.context.registerReceiver(this.networkConnectionStatusBroadcastReceiver, intentFilter);
            AutoReLoginDaemon.getInstance(this.context);
            KeepAliveDaemon.getInstance(this.context);
            LocalUDPDataReciever.getInstance(this.context);
            QoS4ReciveDaemon.getInstance(this.context);
            QoS4SendDaemon.getInstance(this.context);
            this._init = true;
        }

    }

    public void release() {
        LocalUDPSocketProvider.getInstance().closeLocalUDPSocket();
        AutoReLoginDaemon.getInstance(this.context).stop();
        QoS4SendDaemon.getInstance(this.context).stop();
        KeepAliveDaemon.getInstance(this.context).stop();
        LocalUDPDataReciever.getInstance(this.context).stop();
        QoS4ReciveDaemon.getInstance(this.context).stop();
        QoS4SendDaemon.getInstance(this.context).clear();
        QoS4ReciveDaemon.getInstance(this.context).clear();

        try {
            this.context.unregisterReceiver(this.networkConnectionStatusBroadcastReceiver);
        } catch (Exception var2) {
            Log.i(TAG, "还未注册android网络事件广播的监听器，本次取消注册已被正常忽略哦.");
        }

        this._init = false;
        this.setLoginHasInit(false);
        this.setConnectedToServer(false);
    }

    public String getCurrentLoginUserId() {
        return this.currentLoginUserId;
    }

    public ClientCoreSDK setCurrentLoginUserId(String currentLoginUserId) {
        this.currentLoginUserId = currentLoginUserId;
        return this;
    }

    public String getCurrentLoginToken() {
        return this.currentLoginToken;
    }

    public void setCurrentLoginToken(String currentLoginToken) {
        this.currentLoginToken = currentLoginToken;
    }

    public String getCurrentLoginExtra() {
        return this.currentLoginExtra;
    }

    public ClientCoreSDK setCurrentLoginExtra(String currentLoginExtra) {
        this.currentLoginExtra = currentLoginExtra;
        return this;
    }

    public boolean isLoginHasInit() {
        return this.loginHasInit;
    }

    public ClientCoreSDK setLoginHasInit(boolean loginHasInit) {
        this.loginHasInit = loginHasInit;
        return this;
    }

    public boolean isConnectedToServer() {
        return this.connectedToServer;
    }

    public void setConnectedToServer(boolean connectedToServer) {
        this.connectedToServer = connectedToServer;
    }

    public boolean isInitialed() {
        return this._init;
    }

    public boolean isLocalDeviceNetworkOk() {
        return this.localDeviceNetworkOk;
    }

    public void setChatBaseEvent(ChatBaseEvent chatBaseEvent) {
        this.chatBaseEvent = chatBaseEvent;
    }

    public ChatBaseEvent getChatBaseEvent() {
        return this.chatBaseEvent;
    }

    public void setChatTransDataEvent(ChatTransDataEvent chatTransDataEvent) {
        this.chatTransDataEvent = chatTransDataEvent;
    }

    public ChatTransDataEvent getChatTransDataEvent() {
        return this.chatTransDataEvent;
    }

    public void setMessageQoSEvent(MessageQoSEvent messageQoSEvent) {
        this.messageQoSEvent = messageQoSEvent;
    }

    public MessageQoSEvent getMessageQoSEvent() {
        return this.messageQoSEvent;
    }
}
