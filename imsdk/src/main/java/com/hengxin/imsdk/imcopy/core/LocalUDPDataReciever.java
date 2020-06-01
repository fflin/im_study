package com.hengxin.imsdk.imcopy.core;

import android.content.Context;
import android.os.Handler;
import android.os.Message;
import android.util.Log;

import com.hengxin.imsdk.imcopy.ClientCoreSDK;
import com.hengxin.imsdk.imcopy.config.ConfigEntity;
import com.hengxin.imsdk.server.Protocal;
import com.hengxin.imsdk.server.ProtocalFactory;
import com.hengxin.imsdk.server.s.PErrorResponse;
import com.hengxin.imsdk.server.s.PLoginInfoResponse;

import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.util.Observable;
import java.util.Observer;

/**
 * author : fflin
 * date   : 2020/5/29 16:04
 * desc   : 接收消息
 * version: 1.0
 */
public class LocalUDPDataReciever {
    private static final String TAG = LocalUDPDataReciever.class.getSimpleName();
    private static LocalUDPDataReciever instance = null;
    private MessageHandler messageHandler = null;
    private Thread thread = null;
    private boolean init = false;
    private Context context = null;

    public static LocalUDPDataReciever getInstance(Context context) {
        if (instance == null) {
            instance = new LocalUDPDataReciever(context);
        }

        return instance;
    }

    private LocalUDPDataReciever(Context context) {
        this.context = context;
        this.init();
    }

    private void init() {
        if (!this.init) {
            this.messageHandler = new MessageHandler(this.context);
            this.init = true;
        }
    }

    public void stop() {
        if (this.thread != null) {
            this.thread.interrupt();
            this.thread = null;
        }

    }

    /**
     * 登录以后调用这个方法，侦听消息
     */
    public void startup() {
        this.stop();

        try {
            this.thread = new Thread(new Runnable() {
                public void run() {
                    try {
                        if (ClientCoreSDK.DEBUG) {
                            Log.d(LocalUDPDataReciever.TAG, "【IMCORE】本地UDP端口侦听中，端口=" + ConfigEntity.localUDPPort + "...");
                        }

                        LocalUDPDataReciever.this.udpListeningImpl();
                    } catch (Exception var2) {
                        Log.w(LocalUDPDataReciever.TAG, "【IMCORE】本地UDP监听停止了(socket被关闭了?)：" + var2.getMessage() + "，应该是用户退出登陆或网络断开了。");
                    }

                }
            });
            this.thread.start();
        } catch (Exception var2) {
            Log.w(TAG, "【IMCORE】本地UDPSocket监听开启时发生异常," + var2.getMessage(), var2);
        }

    }

    public boolean isInit() {
        return this.init;
    }

    /**
     * 死循环一直在侦听
     * @throws Exception
     */
    private void udpListeningImpl() throws Exception {
        while(true) {
            byte[] data = new byte[1024];
            DatagramPacket packet = new DatagramPacket(data, data.length);
            DatagramSocket localUDPSocket = LocalUDPSocketProvider.getInstance().getLocalUDPSocket();
            if (localUDPSocket != null && !localUDPSocket.isClosed()) {
                localUDPSocket.receive(packet);
                Message m = Message.obtain();
                m.obj = packet;
                this.messageHandler.sendMessage(m);
            }
        }
    }

    /**
     * 处理收到的消息
     */
    private static class MessageHandler extends Handler {
        private Context context = null;

        public MessageHandler(Context context) {
            this.context = context;
        }

        /**
         * 心跳包 {bridge=false, type=51, dataContent='{}', from='0', to='Demo', fp='null', QoS=false, typeu=-1, retryCount=0}
         *
         * 发送的消息是 {"QoS":true,"bridge":false,"dataContent":"999","fp":"c42f7497-cdbe-46b1-be95-de4c9528bb82","from":"Demo","to":"App","type":2,"typeu":-1}
         * 发送消息的回执 {bridge=false, type=4, dataContent='c42f7497-cdbe-46b1-be95-de4c9528bb82', from='App', to='Demo', fp='null', QoS=false, typeu=-1, retryCount=0}
         *
         * 收到的消息 {bridge=false, type=2, dataContent='Good morning 11111111', from='App', to='Demo', fp='715c0889-d9a8-45c8-9b89-71e76a0a32b2', QoS=true, typeu=-1, retryCount=0}
         * @param msg
         */
        public void handleMessage(Message msg) {
            DatagramPacket packet = (DatagramPacket)msg.obj;
            if (packet != null) {
                try {
                    Protocal pFromServer = ProtocalFactory.parse(packet.getData(), packet.getLength());
                    Log.d(LocalUDPDataReciever.TAG, "pFromServer -- "+pFromServer.toString());
                    if (pFromServer.isQoS()) {
                        if (pFromServer.getType() == 50 && ProtocalFactory.parsePLoginInfoResponse(pFromServer.getDataContent()).getCode() != 0) {
                            if (ClientCoreSDK.DEBUG) {
                                Log.d(LocalUDPDataReciever.TAG, "【IMCORE】【BugFIX】这是服务端的登陆返回响应包，且服务端判定登陆失败(即code!=0)，本次无需发送ACK应答包！");
                            }
                        } else {
                            if (QoS4ReciveDaemon.getInstance(this.context).hasRecieved(pFromServer.getFp())) {
                                if (ClientCoreSDK.DEBUG) {
                                    Log.d(LocalUDPDataReciever.TAG, "【IMCORE】【QoS机制】" + pFromServer.getFp() + "已经存在于发送列表中，这是重复包，通知应用层收到该包罗！");
                                }

                                QoS4ReciveDaemon.getInstance(this.context).addRecieved(pFromServer);
                                this.sendRecievedBack(pFromServer);
                                return;
                            }

                            QoS4ReciveDaemon.getInstance(this.context).addRecieved(pFromServer);
                            this.sendRecievedBack(pFromServer);
                        }
                    }

                    switch(pFromServer.getType()) {
                        // 收到的聊天消息 解析并处理消息
                        case 2:
                            if (ClientCoreSDK.getInstance().getChatTransDataEvent() != null) {
                                ClientCoreSDK.getInstance().getChatTransDataEvent().onTransBuffer(pFromServer.getFp(), pFromServer.getFrom(), pFromServer.getDataContent(), pFromServer.getTypeu());
                            }
                            break;
                        case 4:
                            // 收到消息的指纹是发送消息的fp字段
                            String theFingerPrint = pFromServer.getDataContent();
                            if (ClientCoreSDK.DEBUG) {
                                Log.d(LocalUDPDataReciever.TAG, "【IMCORE】【QoS】收到" + pFromServer.getFrom() + "发过来的指纹为" + theFingerPrint + "的应答包.");
                            }

                            if (ClientCoreSDK.getInstance().getMessageQoSEvent() != null) {
                                ClientCoreSDK.getInstance().getMessageQoSEvent().messagesBeReceived(theFingerPrint);
                            }

                            QoS4SendDaemon.getInstance(this.context).remove(theFingerPrint);
                            break;
                        case 50:
                            PLoginInfoResponse loginInfoRes = ProtocalFactory.parsePLoginInfoResponse(pFromServer.getDataContent());
                            if (loginInfoRes.getCode() == 0) {
                                ClientCoreSDK.getInstance().setLoginHasInit(true);
                                AutoReLoginDaemon.getInstance(this.context).stop();
                                KeepAliveDaemon.getInstance(this.context).setNetworkConnectionLostObserver(new Observer() {
                                    public void update(Observable observable, Object data) {
                                        LocalUDPSocketProvider.getInstance().closeLocalUDPSocket();
                                        QoS4ReciveDaemon.getInstance(MessageHandler.this.context).stop();
                                        ClientCoreSDK.getInstance().setConnectedToServer(false);
                                        ClientCoreSDK.getInstance().getChatBaseEvent().onLinkCloseMessage(-1);
                                        AutoReLoginDaemon.getInstance(MessageHandler.this.context).start(true);
                                    }
                                });
                                KeepAliveDaemon.getInstance(this.context).start(false);
                                QoS4SendDaemon.getInstance(this.context).startup(true);
                                QoS4ReciveDaemon.getInstance(this.context).startup(true);
                                ClientCoreSDK.getInstance().setConnectedToServer(true);
                            } else {
                                LocalUDPDataReciever.getInstance(this.context).stop();
                                ClientCoreSDK.getInstance().setConnectedToServer(false);
                            }

                            if (ClientCoreSDK.getInstance().getChatBaseEvent() != null) {
                                ClientCoreSDK.getInstance().getChatBaseEvent().onLoginMessage(loginInfoRes.getCode());
                            }
                            break;
                        case 51:
                            if (ClientCoreSDK.DEBUG) {
                                Log.d(LocalUDPDataReciever.TAG, "【IMCORE】收到服务端回过来的Keep Alive心跳响应包.");
                            }

                            KeepAliveDaemon.getInstance(this.context).updateGetKeepAliveResponseFromServerTimstamp();
                            break;
                        case 52:
                            PErrorResponse errorRes = ProtocalFactory.parsePErrorResponse(pFromServer.getDataContent());
                            if (errorRes.getErrorCode() == 301) {
                                ClientCoreSDK.getInstance().setLoginHasInit(false);
                                Log.e(LocalUDPDataReciever.TAG, "【IMCORE】收到服务端的“尚未登陆”的错误消息，心跳线程将停止，请应用层重新登陆.");
                                KeepAliveDaemon.getInstance(this.context).stop();
                                AutoReLoginDaemon.getInstance(this.context).start(false);
                            }

                            if (ClientCoreSDK.getInstance().getChatTransDataEvent() != null) {
                                ClientCoreSDK.getInstance().getChatTransDataEvent().onErrorResponse(errorRes.getErrorCode(), errorRes.getErrorMsg());
                            }
                            break;
                        default:
                            Log.w(LocalUDPDataReciever.TAG, "【IMCORE】收到的服务端消息类型：" + pFromServer.getType() + "，但目前该类型客户端不支持解析和处理！");
                    }
                } catch (Exception var5) {
                    Log.w(LocalUDPDataReciever.TAG, "【IMCORE】处理消息的过程中发生了错误.", var5);
                }

            }
        }

        private void sendRecievedBack(final Protocal pFromServer) {
            if (pFromServer.getFp() != null) {
                (new LocalUDPDataSender.SendCommonDataAsync(this.context, ProtocalFactory.createRecivedBack(pFromServer.getTo(), pFromServer.getFrom(), pFromServer.getFp(), pFromServer.isBridge())) {
                    protected void onPostExecute(Integer code) {
                        if (ClientCoreSDK.DEBUG) {
                            Log.d(LocalUDPDataReciever.TAG, "【IMCORE】【QoS】向" + pFromServer.getFrom() + "发送" + pFromServer.getFp() + "包的应答包成功,from=" + pFromServer.getTo() + "！");
                        }

                    }
                }).execute(new Object[0]);
            } else {
                Log.w(LocalUDPDataReciever.TAG, "【IMCORE】【QoS】收到" + pFromServer.getFrom() + "发过来需要QoS的包，但它的指纹码却为null！无法发应答包！");
            }

        }
    }
}