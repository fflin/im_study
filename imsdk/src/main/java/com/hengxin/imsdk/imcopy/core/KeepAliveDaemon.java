package com.hengxin.imsdk.imcopy.core;

import android.content.Context;
import android.os.AsyncTask;
import android.os.Handler;
import android.util.Log;

import com.hengxin.imsdk.imcopy.ClientCoreSDK;

import java.util.Observable;
import java.util.Observer;

/**
 * author : fflin
 * date   : 2020/5/27 20:15
 * desc   : 保持连接---心跳---
 * version: 1.0
 */
public class KeepAliveDaemon {

    private static final String TAG = KeepAliveDaemon.class.getSimpleName();
    private static KeepAliveDaemon instance = null;
    public static int NETWORK_CONNECTION_TIME_OUT = 10000;
    public static int KEEP_ALIVE_INTERVAL = 3000;
    private boolean keepAliveRunning = false;
    private long lastGetKeepAliveResponseFromServerTimstamp = 0L;
    private Observer networkConnectionLostObserver = null;
    private Handler handler = null;
    private Runnable runnable = null;
    private boolean _excuting = false;
    private boolean init = false;
    private Context context = null;

    public static KeepAliveDaemon getInstance(Context context) {
        if (instance == null) {
            instance = new KeepAliveDaemon(context);
        }

        return instance;
    }

    private KeepAliveDaemon(Context context) {
        this.context = context;
        this.init();
    }

    private void init() {
        if (!this.init) {
            this.handler = new Handler();
            this.runnable = new Runnable() {
                public void run() {
                    if (!KeepAliveDaemon.this._excuting) {
                        (new AsyncTask<Object, Integer, Integer>() {
                            private boolean willStop = false;

                            protected Integer doInBackground(Object... params) {
                                KeepAliveDaemon.this._excuting = true;
                                if (ClientCoreSDK.DEBUG) {
                                    Log.d(KeepAliveDaemon.TAG, "【IMCORE】心跳线程执行中...");
                                }

                                int code = LocalUDPDataSender.getInstance(KeepAliveDaemon.this.context).sendKeepAlive();
                                return code;
                            }

                            protected void onPostExecute(Integer code) {
                                boolean isInitialedForKeepAlive = KeepAliveDaemon.this.lastGetKeepAliveResponseFromServerTimstamp == 0L;
                                if (isInitialedForKeepAlive) {
                                    KeepAliveDaemon.this.lastGetKeepAliveResponseFromServerTimstamp = System.currentTimeMillis();
                                }

                                if (!isInitialedForKeepAlive) {
                                    long now = System.currentTimeMillis();
                                    if (now - KeepAliveDaemon.this.lastGetKeepAliveResponseFromServerTimstamp >= (long)KeepAliveDaemon.NETWORK_CONNECTION_TIME_OUT) {
                                        KeepAliveDaemon.this.stop();
                                        if (KeepAliveDaemon.this.networkConnectionLostObserver != null) {
                                            KeepAliveDaemon.this.networkConnectionLostObserver.update((Observable)null, (Object)null);
                                        }

                                        this.willStop = true;
                                    }
                                }

                                KeepAliveDaemon.this._excuting = false;
                                if (!this.willStop) {
                                    KeepAliveDaemon.this.handler.postDelayed(KeepAliveDaemon.this.runnable, (long)KeepAliveDaemon.KEEP_ALIVE_INTERVAL);
                                }

                            }
                        }).execute(new Object[0]);
                    }

                }
            };
            this.init = true;
        }
    }

    public void stop() {
        this.handler.removeCallbacks(this.runnable);
        this.keepAliveRunning = false;
        this.lastGetKeepAliveResponseFromServerTimstamp = 0L;
    }

    public void start(boolean immediately) {
        this.stop();
        this.handler.postDelayed(this.runnable, immediately ? 0L : (long)KEEP_ALIVE_INTERVAL);
        this.keepAliveRunning = true;
    }

    public boolean isKeepAliveRunning() {
        return this.keepAliveRunning;
    }

    public boolean isInit() {
        return this.init;
    }

    public void updateGetKeepAliveResponseFromServerTimstamp() {
        this.lastGetKeepAliveResponseFromServerTimstamp = System.currentTimeMillis();
    }

    public void setNetworkConnectionLostObserver(Observer networkConnectionLostObserver) {
        this.networkConnectionLostObserver = networkConnectionLostObserver;
    }
}
