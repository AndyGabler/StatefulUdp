package com.gabler.udpmanager.app;

import com.gabler.udpmanager.client.IUdpClientConfiguration;

/**
 * Default configuration for a UDP client. Implementation of a very basic messaging service.
 *
 * @author Andy Gabler
 */
public class DefaultUdpClientConfig implements IUdpClientConfiguration {

    @Override
    public void handleBytesMessage(byte[] message) {
        System.out.println("Received bytes message.");
    }

    @Override
    public void handleStringMessage(String message) {
        System.out.println("Received message from server: " + message);
    }

    @Override
    public void startAction() {
        System.out.println("Client started");
    }

    @Override
    public void terminationAction() {
        System.out.println("Client terminated.");
    }

    @Override
    public void pauseAction() {
        System.out.println("Client paused.");
    }

    @Override
    public void resumeAction() {
        System.out.println("Client resumed.");
    }
}
