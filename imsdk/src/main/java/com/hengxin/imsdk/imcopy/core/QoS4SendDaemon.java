package com.hengxin.imsdk.imcopy.core;

import android.content.Context;
import android.os.AsyncTask;
import android.os.Handler;
import android.util.Log;

import com.hengxin.imsdk.imcopy.ClientCoreSDK;
import com.hengxin.imsdk.server.Protocal;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.concurrent.ConcurrentHashMap;

/**
 * author : fflin
 * date   : 2020/5/27 20:24
 * desc   : 消息质量保证 进程
 * version: 1.0
 */
public class QoS4SendDaemon {

    private static final String TAG = QoS4SendDaemon.class.getSimpleName();
    private static QoS4SendDaemon instance = null;
    public static final int CHECH_INTERVAL = 5000;
    public static final int MESSAGES_JUST$NOW_TIME = 3000;
    public static final int QOS_TRY_COUNT = 2;
    private ConcurrentHashMap<String, Protocal> sentMessages = new ConcurrentHashMap();
    private ConcurrentHashMap<String, Long> sendMessagesTimestamp = new ConcurrentHashMap();
    private Handler handler = null;
    private Runnable runnable = null;
    private boolean running = false;
    private boolean _excuting = false;
    private boolean init = false;
    private Context context = null;

    public static QoS4SendDaemon getInstance(Context context) {
        if (instance == null) {
            instance = new QoS4SendDaemon(context);
        }

        return instance;
    }

    private QoS4SendDaemon(Context context) {
        this.context = context;
        this.init();
    }

    private void init() {
        if (!this.init) {
            this.handler = new Handler();
            this.runnable = new Runnable() {
                public void run() {
                    if (!QoS4SendDaemon.this._excuting) {
                        (new AsyncTask<Object, Integer, ArrayList<Protocal>>() {
                            private ArrayList<Protocal> lostMessages = new ArrayList();

                            protected ArrayList<Protocal> doInBackground(Object... params) {
                                QoS4SendDaemon.this._excuting = true;

                                try {
                                    if (ClientCoreSDK.DEBUG) {
                                        Log.d(QoS4SendDaemon.TAG, "【IMCORE】【QoS】=========== 消息发送质量保证线程运行中, 当前需要处理的列表长度为" + QoS4SendDaemon.this.sentMessages.size() + "...");
                                    }

                                    Iterator var2 = QoS4SendDaemon.this.sentMessages.keySet().iterator();

                                    while(true) {
                                        while(var2.hasNext()) {
                                            String key = (String)var2.next();
                                            Protocal p = (Protocal)QoS4SendDaemon.this.sentMessages.get(key);
                                            if (p != null && p.isQoS()) {
                                                if (p.getRetryCount() >= 2) {
                                                    if (ClientCoreSDK.DEBUG) {
                                                        Log.d(QoS4SendDaemon.TAG, "【IMCORE】【QoS】指纹为" + p.getFp() + "的消息包重传次数已达" + p.getRetryCount() + "(最多" + 2 + "次)上限，将判定为丢包！");
                                                    }

                                                    this.lostMessages.add((Protocal)p.clone());
                                                    QoS4SendDaemon.this.remove(p.getFp());
                                                } else {
                                                    long delta = System.currentTimeMillis() - (Long)QoS4SendDaemon.this.sendMessagesTimestamp.get(key);
                                                    if (delta <= 3000L) {
                                                        if (ClientCoreSDK.DEBUG) {
                                                            Log.w(QoS4SendDaemon.TAG, "【IMCORE】【QoS】指纹为" + key + "的包距\"刚刚\"发出才" + delta + "ms(<=" + 3000 + "ms将被认定是\"刚刚\"), 本次不需要重传哦.");
                                                        }
                                                    } else {
                                                        (new LocalUDPDataSender.SendCommonDataAsync(QoS4SendDaemon.this.context, p) {
                                                            protected void onPostExecute(Integer code) {
                                                                if (code == 0) {
                                                                    this.p.increaseRetryCount();
                                                                    if (ClientCoreSDK.DEBUG) {
                                                                        Log.d(QoS4SendDaemon.TAG, "【IMCORE】【QoS】指纹为" + this.p.getFp() + "的消息包已成功进行重传，此次之后重传次数已达" + this.p.getRetryCount() + "(最多" + 2 + "次).");
                                                                    }
                                                                } else {
                                                                    Log.w(QoS4SendDaemon.TAG, "【IMCORE】【QoS】指纹为" + this.p.getFp() + "的消息包重传失败，它的重传次数之前已累计为" + this.p.getRetryCount() + "(最多" + 2 + "次).");
                                                                }

                                                            }
                                                        }).execute(new Object[0]);
                                                    }
                                                }
                                            } else {
                                                QoS4SendDaemon.this.remove(key);
                                            }
                                        }

                                        return this.lostMessages;
                                    }
                                } catch (Exception var7) {
                                    Log.w(QoS4SendDaemon.TAG, "【IMCORE】【QoS】消息发送质量保证线程运行时发生异常," + var7.getMessage(), var7);
                                    return this.lostMessages;
                                }
                            }

                            protected void onPostExecute(ArrayList<Protocal> al) {
                                if (al != null && al.size() > 0) {
                                    QoS4SendDaemon.this.notifyMessageLost(al);
                                }

                                QoS4SendDaemon.this._excuting = false;
                                QoS4SendDaemon.this.handler.postDelayed(QoS4SendDaemon.this.runnable, 5000L);
                            }
                        }).execute(new Object[0]);
                    }

                }
            };
            this.init = true;
        }
    }

    protected void notifyMessageLost(ArrayList<Protocal> lostMessages) {
        if (ClientCoreSDK.getInstance().getMessageQoSEvent() != null) {
            ClientCoreSDK.getInstance().getMessageQoSEvent().messagesLost(lostMessages);
        }

    }

    public void startup(boolean immediately) {
        this.stop();
        this.handler.postDelayed(this.runnable, immediately ? 0L : 5000L);
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

    boolean exist(String fingerPrint) {
        return this.sentMessages.get(fingerPrint) != null;
    }

    public void put(Protocal p) {
        if (p == null) {
            Log.w(TAG, "Invalid arg p==null.");
        } else if (p.getFp() == null) {
            Log.w(TAG, "Invalid arg p.getFp() == null.");
        } else if (!p.isQoS()) {
            Log.w(TAG, "This protocal is not QoS pkg, ignore it!");
        } else {
            if (this.sentMessages.get(p.getFp()) != null) {
                Log.w(TAG, "【IMCORE】【QoS】指纹为" + p.getFp() + "的消息已经放入了发送质量保证队列，该消息为何会重复？（生成的指纹码重复？还是重复put？）");
            }

            this.sentMessages.put(p.getFp(), p);
            this.sendMessagesTimestamp.put(p.getFp(), System.currentTimeMillis());
        }
    }

    public void remove(final String fingerPrint) {
        (new AsyncTask() {
            protected Object doInBackground(Object... params) {
                QoS4SendDaemon.this.sendMessagesTimestamp.remove(fingerPrint);
                return QoS4SendDaemon.this.sentMessages.remove(fingerPrint);
            }

            protected void onPostExecute(Object result) {
                Log.w(QoS4SendDaemon.TAG, "【IMCORE】【QoS】指纹为" + fingerPrint + "的消息已成功从发送质量保证队列中移除(可能是收到接收方的应答也可能是达到了重传的次数上限)，重试次数=" + (result != null ? ((Protocal)result).getRetryCount() : "none呵呵."));
            }
        }).execute(new Object[0]);
    }

    public void clear() {
        this.sentMessages.clear();
        this.sendMessagesTimestamp.clear();
    }

    public int size() {
        return this.sentMessages.size();
    }
}
