package com.hengxin.imsdk.imcopy.core;

import android.content.Context;
import android.os.AsyncTask;
import android.os.Handler;
import android.util.Log;

import com.hengxin.imsdk.imcopy.ClientCoreSDK;

/**
 * author : fflin
 * date   : 2020/5/29 16:03
 * desc   : 自动重新登录
 * version: 1.0
 */
public class AutoReLoginDaemon {
    private static final String TAG = AutoReLoginDaemon.class.getSimpleName();
    private static AutoReLoginDaemon instance = null;
    public static int AUTO_RE$LOGIN_INTERVAL = 2000;
    private Handler handler = null;
    private Runnable runnable = null;
    private boolean autoReLoginRunning = false;
    private boolean _excuting = false;
    private boolean init = false;
    private Context context = null;

    public static AutoReLoginDaemon getInstance(Context context) {
        if (instance == null) {
            instance = new AutoReLoginDaemon(context);
        }

        return instance;
    }

    private AutoReLoginDaemon(Context context) {
        this.context = context;
        this.init();
    }

    private void init() {
        if (!this.init) {
            this.handler = new Handler();
            this.runnable = new Runnable() {
                public void run() {
                    if (!AutoReLoginDaemon.this._excuting) {
                        (new AsyncTask<Object, Integer, Integer>() {
                            protected Integer doInBackground(Object... params) {
                                AutoReLoginDaemon.this._excuting = true;
                                if (ClientCoreSDK.DEBUG) {
                                    Log.d(AutoReLoginDaemon.TAG, "【IMCORE】自动重新登陆线程执行中, autoReLogin?" + ClientCoreSDK.autoReLogin + "...");
                                }

                                int code = -1;
                                if (ClientCoreSDK.autoReLogin) {
                                    code = LocalUDPDataSender.getInstance(AutoReLoginDaemon.this.context).sendLogin(ClientCoreSDK.getInstance().getCurrentLoginUserId(), ClientCoreSDK.getInstance().getCurrentLoginToken(), ClientCoreSDK.getInstance().getCurrentLoginExtra());
                                }

                                return code;
                            }

                            protected void onPostExecute(Integer result) {
                                if (result == 0) {
                                    LocalUDPDataReciever.getInstance(AutoReLoginDaemon.this.context).startup();
                                }

                                AutoReLoginDaemon.this._excuting = false;
                                AutoReLoginDaemon.this.handler.postDelayed(AutoReLoginDaemon.this.runnable, (long)AutoReLoginDaemon.AUTO_RE$LOGIN_INTERVAL);
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
        this.autoReLoginRunning = false;
    }

    public void start(boolean immediately) {
        this.stop();
        this.handler.postDelayed(this.runnable, immediately ? 0L : (long)AUTO_RE$LOGIN_INTERVAL);
        this.autoReLoginRunning = true;
    }

    public boolean isAutoReLoginRunning() {
        return this.autoReLoginRunning;
    }

    public boolean isInit() {
        return this.init;
    }
}
