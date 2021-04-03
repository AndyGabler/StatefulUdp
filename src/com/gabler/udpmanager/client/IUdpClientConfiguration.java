package com.gabler.udpmanager.client;

import com.gabler.udpmanager.IUdpNetConfiguration;

/**
 * Configuration for a UDP client.
 *
 * @author Andy Gabler
 */
public interface IUdpClientConfiguration extends IUdpNetConfiguration {

    /**
     * Handle a message that came in as bytes.
     *
     * @param message The message
     */
    void handleBytesMessage(byte[] message);

    /**
     * Handle a message that came in as a String.
     *
     * @param message The message
     */
    void handleStringMessage(String message);
}
