package com.hengxin.imsdk.server;

import com.google.gson.Gson;

import java.util.UUID;

/**
 * author : fflin
 * date   : 2020/5/27 20:19
 * desc   : 数据协议
 * version: 1.0
 */
public class Protocal {
    private boolean bridge;
    private int type;
    private String dataContent;
    private String from;
    private String to;
    private String fp;
    private boolean QoS;
    private int typeu;
    private transient int retryCount;

    public Protocal(int type, String dataContent, String from, String to) {
        this(type, dataContent, from, to, -1);
    }

    public Protocal(int type, String dataContent, String from, String to, int typeu) {
        this(type, dataContent, from, to, false, (String)null, typeu);
    }

    public Protocal(int type, String dataContent, String from, String to, boolean QoS, String fingerPrint) {
        this(type, dataContent, from, to, QoS, fingerPrint, -1);
    }

    public Protocal(int type, String dataContent, String from, String to, boolean QoS, String fingerPrint, int typeu) {
        this.bridge = false;
        this.type = 0;
        this.dataContent = null;
        this.from = "-1";
        this.to = "-1";
        this.fp = null;
        this.QoS = false;
        this.typeu = -1;
        this.retryCount = 0;
        this.type = type;
        this.dataContent = dataContent;
        this.from = from;
        this.to = to;
        this.QoS = QoS;
        this.typeu = typeu;
        if (QoS && fingerPrint == null) {
            this.fp = genFingerPrint();
        } else {
            this.fp = fingerPrint;
        }

    }

    public int getType() {
        return this.type;
    }

    public void setType(int type) {
        this.type = type;
    }

    public String getDataContent() {
        return this.dataContent;
    }

    public void setDataContent(String dataContent) {
        this.dataContent = dataContent;
    }

    public String getFrom() {
        return this.from;
    }

    public void setFrom(String from) {
        this.from = from;
    }

    public String getTo() {
        return this.to;
    }

    public void setTo(String to) {
        this.to = to;
    }

    public String getFp() {
        return this.fp;
    }

    public int getRetryCount() {
        return this.retryCount;
    }

    public void increaseRetryCount() {
        ++this.retryCount;
    }

    public boolean isQoS() {
        return this.QoS;
    }

    public void setQoS(boolean qoS) {
        this.QoS = qoS;
    }

    public boolean isBridge() {
        return this.bridge;
    }

    public void setBridge(boolean bridge) {
        this.bridge = bridge;
    }

    public int getTypeu() {
        return this.typeu;
    }

    public void setTypeu(int typeu) {
        this.typeu = typeu;
    }

    public String toGsonString() {
        return (new Gson()).toJson(this);
    }

    public byte[] toBytes() {
        return CharsetHelper.getBytes(this.toGsonString());
    }

    public Object clone() {
        Protocal cloneP = new Protocal(this.getType(), this.getDataContent(), this.getFrom(), this.getTo(), this.isQoS(), this.getFp());
        cloneP.setBridge(this.bridge);
        cloneP.setTypeu(this.typeu);
        return cloneP;
    }

    public static String genFingerPrint() {
        return UUID.randomUUID().toString();
    }
}
