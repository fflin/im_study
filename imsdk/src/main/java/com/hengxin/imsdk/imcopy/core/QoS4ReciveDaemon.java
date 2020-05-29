package com.hengxin.imsdk.imcopy.core;

import android.content.Context;
import android.os.Handler;
import android.util.Log;

import com.hengxin.imsdk.imcopy.ClientCoreSDK;
import com.hengxin.imsdk.server.Protocal;

import java.util.Iterator;
import java.util.concurrent.ConcurrentHashMap;

/**
 * author : fflin
 * date   : 2020/5/29 16:04
 * desc   :
 * version: 1.0
 */
public class QoS4ReciveDaemon {
    private static final String TAG = QoS4ReciveDaemon.class.getSimpleName();
    private static QoS4ReciveDaemon instance = null;
    public static final int CHECH_INTERVAL = 300000;
    public static final int MESSAGES_VALID_TIME = 600000;
    private ConcurrentHashMap<String, Long> recievedMessages = new ConcurrentHashMap();
    private Handler handler = null;
    private Runnable runnable = null;
    private boolean running = false;
    private boolean _excuting = false;
    private boolean init = false;
    private Context context = null;

    public static QoS4ReciveDaemon getInstance(Context context) {
        if (instance == null) {
            instance = new QoS4ReciveDaemon(context);
        }

        return instance;
    }

    public QoS4ReciveDaemon(Context context) {
        this.context = context;
        this.init();
    }

    private void init() {
        if (!this.init) {
            this.handler = new Handler();
            this.runnable = new Runnable() {
                public void run() {
                    if (!QoS4ReciveDaemon.this._excuting) {
                        QoS4ReciveDaemon.this._excuting = true;
                        if (ClientCoreSDK.DEBUG) {
                            Log.d(QoS4ReciveDaemon.TAG, "【IMCORE】【QoS接收方】++++++++++ START 暂存处理线程正在运行中，当前长度" + QoS4ReciveDaemon.this.recievedMessages.size() + ".");
                        }

                        Iterator var1 = QoS4ReciveDaemon.this.recievedMessages.keySet().iterator();

                        while(var1.hasNext()) {
                            String key = (String)var1.next();
                            long delta = System.currentTimeMillis() - (Long)QoS4ReciveDaemon.this.recievedMessages.get(key);
                            if (delta >= 600000L) {
                                if (ClientCoreSDK.DEBUG) {
                                    Log.d(QoS4ReciveDaemon.TAG, "【IMCORE】【QoS接收方】指纹为" + key + "的包已生存" + delta + "ms(最大允许" + 600000 + "ms), 马上将删除之.");
                                }

                                QoS4ReciveDaemon.this.recievedMessages.remove(key);
                            }
                        }
                    }

                    if (ClientCoreSDK.DEBUG) {
                        Log.d(QoS4ReciveDaemon.TAG, "【IMCORE】【QoS接收方】++++++++++ END 暂存处理线程正在运行中，当前长度" + QoS4ReciveDaemon.this.recievedMessages.size() + ".");
                    }

                    QoS4ReciveDaemon.this._excuting = false;
                    QoS4ReciveDaemon.this.handler.postDelayed(QoS4ReciveDaemon.this.runnable, 300000L);
                }
            };
            this.init = true;
        }
    }

    public void startup(boolean immediately) {
        this.stop();
        if (this.recievedMessages != null && this.recievedMessages.size() > 0) {
            Iterator var2 = this.recievedMessages.keySet().iterator();

            while(var2.hasNext()) {
                String key = (String)var2.next();
                this.putImpl(key);
            }
        }

        this.handler.postDelayed(this.runnable, immediately ? 0L : 300000L);
        this.running = true;
    }

    public void stop() {
        this.handler.removeCallbacks(this.runnable);
        this.running = false;
    }

    public boolean isRunning() {
        return this.running;
    }

    public boolean isInit() {
        return this.init;
    }

    public void addRecieved(Protocal p) {
        if (p != null && p.isQoS()) {
            this.addRecieved(p.getFp());
        }

    }

    public void addRecieved(String fingerPrintOfProtocal) {
        if (fingerPrintOfProtocal == null) {
            Log.w(TAG, "【IMCORE】无效的 fingerPrintOfProtocal==null!");
        } else {
            if (this.recievedMessages.containsKey(fingerPrintOfProtocal)) {
                Log.w(TAG, "【IMCORE】【QoS接收方】指纹为" + fingerPrintOfProtocal + "的消息已经存在于接收列表中，该消息重复了（原理可能是对方因未收到应答包而错误重传导致），更新收到时间戳哦.");
            }

            this.putImpl(fingerPrintOfProtocal);
        }
    }

    private void putImpl(String fingerPrintOfProtocal) {
        if (fingerPrintOfProtocal != null) {
            this.recievedMessages.put(fingerPrintOfProtocal, System.currentTimeMillis());
        }

    }

    public boolean hasRecieved(String fingerPrintOfProtocal) {
        return this.recievedMessages.containsKey(fingerPrintOfProtocal);
    }

    public void clear() {
        this.recievedMessages.clear();
    }

    public int size() {
        return this.recievedMessages.size();
    }
}
