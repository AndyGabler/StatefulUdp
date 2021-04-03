package com.gabler.udpmanager.server;

import com.gabler.udpmanager.ByteToUdpRequestTransformer;
import com.gabler.udpmanager.model.UdpRequest;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.util.function.Function;

/**
 * Listening thread for a server. Listens for messages from a client and posts them back to the server.
 *
 * @author Andy Gabler
 */
public class UdpServerListeningThread extends Thread {

    private final Function<byte[], UdpRequest> bytesToUdpRequestTransformer;

    private volatile boolean terminated = false;
    private volatile boolean listening = false;
    private final UdpServer server;
    private final DatagramSocket socket;

    /**
     * Initialize a listening thread for a server
     *
     * @param server The server to post back to
     * @param socket The socket to listen to
     */
    public UdpServerListeningThread(UdpServer server, DatagramSocket socket) {
        this(server, socket, new ByteToUdpRequestTransformer());
    }

    /**
     * Initialize a listening thread for a server
     *
     * @param server The server to post back to
     * @param socket The socket to listen to
     * @param aBytesToUdpRequestTransformer Transformer for turning bytes to a {@link UdpRequest}
     */
    public UdpServerListeningThread(UdpServer server, DatagramSocket socket, Function<byte[], UdpRequest> aBytesToUdpRequestTransformer) {
        this.server = server;
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
                System.out.println("I/O on socket receive.");
                continue;
            }

            final InetAddress sentAddress = receivedPacket.getAddress();
            final int clientPort = receivedPacket.getPort();

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
                server.handleMessageFromClient(request, sentAddress, clientPort);
            }
        }
    }
}
