package com.hengxin.imsdk.imcopy.core;

import android.content.Context;
import android.os.AsyncTask;
import android.util.Log;

import com.hengxin.imsdk.imcopy.ClientCoreSDK;
import com.hengxin.imsdk.imcopy.config.ConfigEntity;
import com.hengxin.imsdk.imcopy.util.UDPUtils;
import com.hengxin.imsdk.server.Protocal;
import com.hengxin.imsdk.server.ProtocalFactory;

import java.net.DatagramSocket;
import java.net.InetAddress;

/**
 * author : fflin
 * date   : 2020/5/27 20:10
 * desc   : 核心类。各种封装后，在这里发消息
 * version: 1.0
 */
public class LocalUDPDataSender {
    private static final String TAG = LocalUDPDataSender.class.getSimpleName();
    private static LocalUDPDataSender instance = null;
    private Context context = null;

    public static LocalUDPDataSender getInstance(Context context) {
        if (instance == null) {
            instance = new LocalUDPDataSender(context);
        }

        return instance;
    }

    private LocalUDPDataSender(Context context) {
        this.context = context;
    }

    int sendLogin(String loginUserId, String loginToken, String extra) {
        byte[] b = ProtocalFactory.createPLoginInfo(loginUserId, loginToken, extra).toBytes();
        int code = this.send(b, b.length);
        if (code == 0) {
            ClientCoreSDK.getInstance().setCurrentLoginUserId(loginUserId);
            ClientCoreSDK.getInstance().setCurrentLoginToken(loginToken);
            ClientCoreSDK.getInstance().setCurrentLoginExtra(extra);
        }

        return code;
    }

    public int sendLoginout() {
        int code = 0;
        if (ClientCoreSDK.getInstance().isLoginHasInit()) {
            byte[] b = ProtocalFactory.createPLoginoutInfo(ClientCoreSDK.getInstance().getCurrentLoginUserId()).toBytes();
            code = this.send(b, b.length);
            if (code == 0) {
            }
        }

        ClientCoreSDK.getInstance().release();
        return code;
    }

    int sendKeepAlive() {
        byte[] b = ProtocalFactory.createPKeepAlive(ClientCoreSDK.getInstance().getCurrentLoginUserId()).toBytes();
        return this.send(b, b.length);
    }

    public int sendCommonData(String dataContentWidthStr, String to_user_id) {
        return this.sendCommonData(dataContentWidthStr, to_user_id, -1);
    }

    public int sendCommonData(String dataContentWidthStr, String to_user_id, int typeu) {
        return this.sendCommonData(dataContentWidthStr, to_user_id, (String)null, typeu);
    }

    public int sendCommonData(String dataContentWidthStr, String to_user_id, String fingerPrint, int typeu) {
        return this.sendCommonData(dataContentWidthStr, to_user_id, true, fingerPrint, typeu);
    }

    public int sendCommonData(String dataContentWidthStr, String to_user_id, boolean QoS, String fingerPrint, int typeu) {
        return this.sendCommonData(ProtocalFactory.createCommonData(dataContentWidthStr, ClientCoreSDK.getInstance().getCurrentLoginUserId(), to_user_id, QoS, fingerPrint, typeu));
    }

    public int sendCommonData(Protocal p) {
        if (p != null) {
            byte[] b = p.toBytes();
            int code = this.send(b, b.length);
            if (code == 0 && p.isQoS() && !QoS4SendDaemon.getInstance(this.context).exist(p.getFp())) {
                QoS4SendDaemon.getInstance(this.context).put(p);
            }

            return code;
        } else {
            return 4;
        }
    }

    // 核心 发送数据包
    private int send(byte[] fullProtocalBytes, int dataLen) {
        if (!ClientCoreSDK.getInstance().isInitialed()) {
            return 203;
        } else if (!ClientCoreSDK.getInstance().isLocalDeviceNetworkOk()) {
            Log.e(TAG, "【IMCORE】本地网络不能工作，send数据没有继续!");
            return 204;
        } else {
            DatagramSocket ds = LocalUDPSocketProvider.getInstance().getLocalUDPSocket();
            if (ds != null && !ds.isConnected()) {
                try {
                    if (ConfigEntity.serverIP == null) {
                        Log.w(TAG, "【IMCORE】send数据没有继续，原因是ConfigEntity.server_ip==null!");
                        return 205;
                    }

                    ds.connect(InetAddress.getByName(ConfigEntity.serverIP), ConfigEntity.serverUDPPort);
                } catch (Exception var5) {
                    Log.w(TAG, "【IMCORE】send时出错，原因是：" + var5.getMessage(), var5);
                    return 202;
                }
            }

            return UDPUtils.send(ds, fullProtocalBytes, dataLen) ? 0 : 3;
        }
    }

    public abstract static class SendLoginDataAsync extends AsyncTask<Object, Integer, Integer> {
        protected Context context;
        protected String loginUserId;
        protected String loginToken;
        protected String extra;

        public SendLoginDataAsync(Context context, String loginUserId, String loginToken) {
            this(context, loginUserId, loginToken, (String)null);
        }

        public SendLoginDataAsync(Context context, String loginUserId, String loginToken, String extra) {
            this.context = null;
            this.loginUserId = null;
            this.loginToken = null;
            this.extra = null;
            this.context = context;
            this.loginUserId = loginUserId;
            this.loginToken = loginToken;
            this.extra = extra;
        }

        protected Integer doInBackground(Object... params) {
            int code = LocalUDPDataSender.getInstance(this.context).sendLogin(this.loginUserId, this.loginToken, this.extra);
            return code;
        }

        protected void onPostExecute(Integer code) {
            if (code == 0) {
                LocalUDPDataReciever.getInstance(this.context).startup();
            } else {
                Log.d(LocalUDPDataSender.TAG, "【IMCORE】数据发送失败, 错误码是：" + code + "！");
            }

            this.fireAfterSendLogin(code);
        }

        protected void fireAfterSendLogin(int code) {
        }
    }

    public abstract static class SendCommonDataAsync extends AsyncTask<Object, Integer, Integer> {
        protected Context context;
        protected Protocal p;

        public SendCommonDataAsync(Context context, String dataContentWidthStr, String to_user_id) {
            this(context, dataContentWidthStr, to_user_id, (String)null, -1);
        }

        public SendCommonDataAsync(Context context, String dataContentWidthStr, String to_user_id, int typeu) {
            this(context, dataContentWidthStr, to_user_id, (String)null, typeu);
        }

        public SendCommonDataAsync(Context context, String dataContentWidthStr, String to_user_id, String fingerPrint, int typeu) {
            this(context, ProtocalFactory.createCommonData(dataContentWidthStr, ClientCoreSDK.getInstance().getCurrentLoginUserId(), to_user_id, true, fingerPrint, typeu));
        }

        public SendCommonDataAsync(Context context, Protocal p) {
            this.context = null;
            this.p = null;
            if (p == null) {
                Log.w(LocalUDPDataSender.TAG, "【IMCORE】无效的参数p==null!");
            } else {
                this.context = context;
                this.p = p;
            }
        }

        protected Integer doInBackground(Object... params) {
            return this.p != null ? LocalUDPDataSender.getInstance(this.context).sendCommonData(this.p) : -1;
        }

        protected abstract void onPostExecute(Integer var1);
    }
}
