package com.hengxin.imsdk.server;

import java.io.UnsupportedEncodingException;
import java.nio.charset.Charset;
import java.nio.charset.CharsetDecoder;

/**
 * author : fflin
 * date   : 2020/5/27 20:20
 * desc   : 编解码
 * version: 1.0
 */
public class CharsetHelper {

    public static final CharsetDecoder decoder = Charset.forName("UTF-8").newDecoder();
    public static final String ENCODE_CHARSET = "UTF-8";
    public static final String DECODE_CHARSET = "UTF-8";

    public CharsetHelper() {
    }

    public static String getString(byte[] b, int len) {
        try {
            return new String(b, 0, len, "UTF-8");
        } catch (UnsupportedEncodingException var3) {
            return new String(b, 0, len);
        }
    }

    public static String getString(byte[] b, int start, int len) {
        try {
            return new String(b, start, len, "UTF-8");
        } catch (UnsupportedEncodingException var4) {
            return new String(b, start, len);
        }
    }

    public static byte[] getBytes(String str) {
        if (str != null) {
            try {
                return str.getBytes("UTF-8");
            } catch (UnsupportedEncodingException var2) {
                return str.getBytes();
            }
        } else {
            return new byte[0];
        }
    }
}
