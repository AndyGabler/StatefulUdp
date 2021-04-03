package com.gabler.udpmanager.server;

import com.gabler.udpmanager.ByteToUdpRequestTransformer;
import com.gabler.udpmanager.model.UdpRequest;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.util.function.Function;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Listening thread for a server. Listens for messages from a client and posts them back to the server.
 *
 * @author Andy Gabler
 */
public class UdpServerListeningThread extends Thread {

    private static final Logger LOGGER = Logger.getLogger("UdpServerListeningThread");

    private final Function<byte[], UdpRequest> bytesToUdpRequestTransformer;

    private final int listenerId;
    private volatile boolean terminated = false;
    private volatile boolean listening = false;
    private final UdpServer server;
    private final DatagramSocket socket;

    /**
     * Initialize a listening thread for a server
     *
     * @param server The server to post back to
     * @param socket The socket to listen to
     * @param listenerId Integer identifier for this thread
     */
    public UdpServerListeningThread(UdpServer server, DatagramSocket socket, int listenerId) {
        this(server, socket, listenerId, new ByteToUdpRequestTransformer());
    }

    /**
     * Initialize a listening thread for a server
     *
     * @param server The server to post back to
     * @param socket The socket to listen to
     * @param listenerId Integer identifier for this thread
     * @param aBytesToUdpRequestTransformer Transformer for turning bytes to a {@link UdpRequest}
     */
    public UdpServerListeningThread(UdpServer server, DatagramSocket socket, int listenerId, Function<byte[], UdpRequest> aBytesToUdpRequestTransformer) {
        this.server = server;
        this.socket = socket;
        this.listenerId = listenerId;
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
        final String id = "[Thread " + listenerId + "] ";
        LOGGER.info(id + "Server listening thread started.");

        while (!terminated) {

            if (!listening) {
                continue;
            }

            final byte[] buffer = new byte[65535];
            final DatagramPacket receivedPacket = new DatagramPacket(buffer, buffer.length);
            try {
                socket.receive(receivedPacket);
            } catch (IOException exception) {
                // Common exit case on closure, much ado about nothing
                LOGGER.log(Level.SEVERE, id + "IO exception on socket receive.", exception);
                continue;
            }

            final InetAddress sentAddress = receivedPacket.getAddress();
            final int clientPort = receivedPacket.getPort();

            LOGGER.fine(id + "Received client message.");
            UdpRequest request;
            try {
                request = bytesToUdpRequestTransformer.apply(buffer);
            } catch (RuntimeException exception) {
                // Means we were sent weird packet by bad client. Don't care.
                LOGGER.log(Level.SEVERE, id + "Could not serialize bytes message to UdpRequest.", exception);
                continue;
            }

            if (request != null) {
                try {
                    server.handleMessageFromClient(request, sentAddress, clientPort);
                } catch (Exception exception) {
                    LOGGER.log(Level.SEVERE, id + "Failed to handle UDP request.", exception);
                }
            }
        }

        LOGGER.info(id + " Terminated.");
    }
}
