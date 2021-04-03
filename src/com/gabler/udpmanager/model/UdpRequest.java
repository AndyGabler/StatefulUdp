package com.gabler.udpmanager.model;

import java.io.Serializable;

/**
 * A request to be sent between client and server. It it assumed that any information on this class does not need to
 * be encrypted since it is metadata where spoofing or eavesdropping is forgiveable.
 *
 * @author Andy Gabler
 */
public class UdpRequest implements Serializable {

    public static final int PAYLOAD_TYPE_STRING = 0;
    public static final int PAYLOAD_TYPE_BYTES = 1;

    private int payloadType;
    private String stringPayload;
    private byte[] bytePayload;
    private String keyId;

    public int getPayloadType() {
        return payloadType;
    }

    public void setPayloadType(int type) {
        payloadType = type;
    }

    public String getStringPayload() {
        return stringPayload;
    }

    public void setStringPayload(String payload) {
        stringPayload = payload;
    }

    public byte[] getBytePayload() {
        return bytePayload;
    }

    public void setBytePayload(byte[] payload) {
        bytePayload = payload;
    }

    public String getKeyId() {
        return keyId;
    }

    public void setKeyId(String id) {
        keyId = id;
    }
}
