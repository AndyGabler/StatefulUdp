package com.gabler.udpmanager.client;

import com.gabler.udpmanager.ByteToUdpRequestTransformer;
import com.gabler.udpmanager.model.UdpRequest;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.util.function.Function;

/**
 * Listening thread for a client. Listens for messages from the server and posts them back to the client.
 *
 * @author Andy Gabler
 */
public class UdpClientListeningThread extends Thread {

    private final Function<byte[], UdpRequest> bytesToUdpRequestTransformer;

    private volatile boolean terminated = false;
    private volatile boolean listening = false;
    private final UdpClient client;
    private final DatagramSocket socket;

    /**
     * Initialize a listening thread for a client.
     *
     * @param client The client to post back to
     * @param socket The socket to listen to
     */
    public UdpClientListeningThread(UdpClient client, DatagramSocket socket) {
        this(client, socket, new ByteToUdpRequestTransformer());
    }

    /**
     * Initialize a listening thread for a client.
     *
     * @param client The client to post back to
     * @param socket The socket to listen to
     * @param aBytesToUdpRequestTransformer Transformer for turning bytes to a {@link UdpRequest}
     */
    public UdpClientListeningThread(UdpClient client, DatagramSocket socket, Function<byte[], UdpRequest> aBytesToUdpRequestTransformer) {
        this.client = client;
        this.socket = socket;
        bytesToUdpRequestTransformer = aBytesToUdpRequestTransformer;
    }

    /**
     * Start listening to the socket.
     */
    public synchronized void startListen() {
        listening = true;
    }

    /**
     * Stop listening to the socket.
     */
    public synchronized void stopListen() {
        listening = false;
    }

    /**
     * Kill the listener.
     */
    public synchronized void killListener() {
        terminated = true;
    }

    public void run() {
        while (!terminated) {

            if (!listening) {
                continue;
            }

            final byte[] buffer = new byte[65535];
            final DatagramPacket receivedPacket = new DatagramPacket(buffer, buffer.length);
            try {
                socket.receive(receivedPacket);
            } catch (IOException exception) {
                // TODO
                System.out.println("IO on socket receive.");
                continue;
            }

            UdpRequest request;
            try {
                request = bytesToUdpRequestTransformer.apply(buffer);
            } catch (RuntimeException e) {
                // TODO
                System.out.println(e);
                System.out.println("RQ transform failed");
                continue;
            }

            if (request != null) {
                client.handleMessageFromServer(request);
            }
        }
    }
}
