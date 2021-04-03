package com.gabler.udpmanager.app;

import com.gabler.udpmanager.server.IUdpServerConfiguration;
import com.gabler.udpmanager.server.ServerClientCallback;

/**
 * Default configuration for a UDP server. Implementation of a very basic messaging service.
 *
 * @author Andy Gabler
 */
public class DefaultUdpServerConfig implements IUdpServerConfiguration {

    @Override
    public void handleBytesMessage(byte[] message, ServerClientCallback callback) {
        String clientId = "[anonymous]";
        if (callback != null) {
            clientId = "[" + callback.getAddress().getHostName() + "(" + callback.getPortNumber() + ")]";
        }
        System.out.println(clientId + " sent bytes message");
    }

    @Override
    public void handleStringMessage(String message, ServerClientCallback callback) {
        String clientId = "[anonymous]";
        if (callback != null) {
            clientId = "[" + callback.getAddress().getHostName() + "(" + callback.getPortNumber() + ")]";
        }
        System.out.println(clientId + " sent message " + message);
    }

    @Override
    public void startAction() {
        System.out.println("Server started");
    }

    @Override
    public void terminationAction() {
        System.out.println("Server terminated.");
    }

    @Override
    public void pauseAction() {
        System.out.println("Server paused.");
    }

    @Override
    public void resumeAction() {
        System.out.println("Server resumed.");
    }
}
