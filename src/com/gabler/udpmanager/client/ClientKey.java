package com.gabler.udpmanager.client;

/**
 * Client-side tracking of a key the client uses on the server.
 *
 * @author Andy Gabler
 */
public class ClientKey {

    private String keyId;
    private byte[] keyBytes;

    public String getKeyId() {
        return keyId;
    }

    public void setKeyId(String keyId) {
        this.keyId = keyId;
    }

    public byte[] getKeyBytes() {
        return keyBytes;
    }

    public void setKeyBytes(byte[] keyBytes) {
        this.keyBytes = keyBytes;
    }
}
