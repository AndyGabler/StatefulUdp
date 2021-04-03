package com.gabler.udpmanager.server;

import com.gabler.udpmanager.IUdpNetConfiguration;

/**
 * Configuration for a UDP server.
 *
 * @author Andy Gabler
 */
public interface IUdpServerConfiguration extends IUdpNetConfiguration {

    /**
     * Handle a message that came in as bytes.
     *
     * @param message The message
     * @param callback Identifying information about the client who sent the message
     */
    void handleBytesMessage(byte[] message, ServerClientCallback callback);

    /**
     * Handle a message that came in as a String.
     *
     * @param message The message
     * @param callback Identifying information about the client who sent the message
     */
    void handleStringMessage(String message, ServerClientCallback callback);
}
