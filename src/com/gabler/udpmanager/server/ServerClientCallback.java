package com.gabler.udpmanager.server;

import java.net.InetAddress;

/**
 * The record a server has about a client.
 *
 * @author Andy Gabler
 */
public class ServerClientCallback {

    private InetAddress address;
    private int portNumber;
    private String keyId;

    public InetAddress getAddress() {
        return address;
    }

    public void setAddress(InetAddress address) {
        this.address = address;
    }

    public int getPortNumber() {
        return portNumber;
    }

    public void setPortNumber(int portNumber) {
        this.portNumber = portNumber;
    }

    public String getKeyId() {
        return keyId;
    }

    public void setKeyId(String keyId) {
        this.keyId = keyId;
    }
}
