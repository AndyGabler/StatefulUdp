package com.gabler.udpmanager;

/**
 * Configuration for either a client or server.
 */
public interface IUdpNetConfiguration {

    /**
     * The action to take when client or server started.
     */
    void startAction();

    /**
     * The action to take when client or server terminated.
     */
    void terminationAction();

    /**
     * The action to take when client or server paused.
     */
    void pauseAction();

    /**
     * The action to take when client or server resumed.
     */
    void resumeAction();
}
