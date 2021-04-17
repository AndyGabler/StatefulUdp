package com.gabler.udpmanager.client;

import com.gabler.udpmanager.ByteToUdpRequestTransformer;
import com.gabler.udpmanager.model.UdpRequest;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.util.function.Function;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Listening thread for a client. Listens for messages from the server and posts them back to the client.
 *
 * @author Andy Gabler
 */
public class UdpClientListeningThread extends Thread {

    private static final Logger LOGGER = Logger.getLogger("UdpClientListeningThread");

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
    public void startListen() {
        listening = true;
    }

    /**
     * Stop listening to the socket.
     */
    public void stopListen() {
        listening = false;
    }

    /**
     * Kill the listener.
     */
    public void killListener() {
        terminated = true;
    }

    public void run() {
        LOGGER.info("Listening thread for messages coming back from server started on " + socket.getLocalAddress() + "(" + socket.getLocalPort() + ").");
        while (!terminated) {

            if (!listening) {
                continue;
            }

            final byte[] buffer = new byte[65535];
            final DatagramPacket receivedPacket = new DatagramPacket(buffer, buffer.length);
            try {
                socket.receive(receivedPacket);
            } catch (IOException exception) {
                // Common exit case on closure
                LOGGER.log(Level.SEVERE, "IO exception on socket receive.", exception);
                continue;
            }

            UdpRequest request;
            try {
                request = bytesToUdpRequestTransformer.apply(buffer);
            } catch (RuntimeException exception) {
                // Post back to this port failed
                LOGGER.log(Level.SEVERE, "Could not serialize bytes message to UdpRequest.", exception);
                continue;
            }

            if (request != null) {
                try {
                    client.handleMessageFromServer(request);
                } catch (Exception exception) {
                    LOGGER.log(Level.SEVERE, "Failed to handle UDP request.", exception);
                }
            }
        }
        LOGGER.info("Client listener terminated.");
    }
}
