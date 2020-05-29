package com.hengxin.imsdk.imcopy.util;

import android.util.Log;

import com.hengxin.imsdk.server.CharsetHelper;

import java.net.DatagramPacket;
import java.net.DatagramSocket;

/**
 * author : fflin
 * date   : 2020/5/29 16:08
 * desc   :
 * version: 1.0
 */
public class UDPUtils {

    private static final String TAG = UDPUtils.class.getSimpleName();

    public UDPUtils() {
    }

    /**
     * 发送消息，消息已经转为byte数组，转byte前的bean
     * @see com.hengxin.imsdk.server.Protocal
     * @param skt 使用UDP连接的socket，客户端不需要先连接数据，可以直接发送给服务端
     * @param d json转换后的byte
     * @param dataLen byte长度
     * @return 发送是否成功
     */
    public static boolean send(DatagramSocket skt, byte[] d, int dataLen) {
        if (skt != null && d != null) {
            try {
                String msg = CharsetHelper.getString(d, dataLen);
                /**
                 * 登录消息(登录账户用户名是 你)
                 * {"QoS":false,"bridge":false,"dataContent":"59668ee6-386d-41fa-964f-3da6570e336d","from":"你","to":"0","type":4,"typeu":-1}
                 */

                /**
                 * 向用户名是123的用户发送了一条？？？的消息
                 * {"QoS":true,"bridge":false,"dataContent":"？？？","fp":"5815a87b-6ea1-46fa-813e-189497b381d4","from":"你","to":"123","type":2,"typeu":-1}
                 */

                /**
                 * 心跳消息
                 * {"QoS":false,"bridge":false,"dataContent":"{}","from":"你","to":"0","type":1,"typeu":-1}
                 */
                Log.e(TAG, "【IMCORE】send方法中》》 发送的消息是 " + msg);
                return send(skt, new DatagramPacket(d, dataLen));
            } catch (Exception var4) {
                Log.e(TAG, "【IMCORE】send方法中》》发送UDP数据报文时出错了：remoteIp=" + skt.getInetAddress() + ", remotePort=" + skt.getPort() + ".原因是：" + var4.getMessage(), var4);
                return false;
            }
        } else {
            Log.e(TAG, "【IMCORE】send方法中》》无效的参数：skt=" + skt);
            return false;
        }
    }

    public static synchronized boolean send(DatagramSocket skt, DatagramPacket p) {
        boolean sendSucess = true;
        if (skt != null && p != null) {
            if (skt.isConnected()) {
                try {
                    skt.send(p);
                } catch (Exception var4) {
                    sendSucess = false;
                    Log.e(TAG, "【IMCORE】send方法中》》发送UDP数据报文时出错了，原因是：" + var4.getMessage(), var4);
                }
            }
        } else {
            Log.w(TAG, "【IMCORE】在send()UDP数据报时没有成功执行，原因是：skt==null || p == null!");
        }

        return sendSucess;
    }
}
