package com.gabler.udpmanager.server;

import com.gabler.udpmanager.LifeCycleState;
import com.gabler.udpmanager.ResourceLock;
import com.gabler.udpmanager.model.UdpRequest;

import java.io.ByteArrayOutputStream;
import java.io.ObjectOutputStream;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.SocketException;
import java.util.ArrayList;
import java.util.Optional;

/**
 * Abstraction for the management of a UDP socket acting as a server.
 *
 * @author Andy Gabler
 */
public class UdpServer {

    /*
     * TODO
     * Few considerations here, first off, this is Spring Boot's thread pool. This appropriate for games?
     * Secondly, this could be a property of some kind?
     */
    private static final int THREAD_POOL_SIZE = 30;

    private IUdpServerConfiguration configuration;
    private volatile LifeCycleState lifecycleState;

    private final DatagramSocket socket;
    private final ArrayList<UdpServerListeningThread> listeningThreads;
    private final ResourceLock<ArrayList<ServerClientCallback>> clientCallbacks;

    /**
     * Initialize an abstraction
     *
     * @param portNumber The server's port
     * @throws SocketException If the port cannot be used
     */
    public UdpServer(int portNumber) throws SocketException {
        this.lifecycleState = LifeCycleState.INITIALIZED;
        socket = new DatagramSocket(portNumber);
        listeningThreads = new ArrayList<>();
        clientCallbacks = new ResourceLock<>(new ArrayList<>());

        /*
         * It is possible for multiple threads to receive from the same DatagramSocket, but only one of them will get
         * each packet. Create listening threads.
         */
        for (int counter = 0; THREAD_POOL_SIZE > counter; counter++) {
            listeningThreads.add(new UdpServerListeningThread(this, socket));
        }
    }

    /**
     * Set the server configuration being used.
     *
     * @param configuration The configuration.
     */
    public void setConfiguration(IUdpServerConfiguration configuration) {
        checkLifeCycleTooMature(LifeCycleState.READY);
        this.lifecycleState = LifeCycleState.READY;
        this.configuration = configuration;
    }

    /**
     * Start the server.
     */
    public void start() {
        checkLifeCycleMatureEnough(LifeCycleState.READY);
        checkLifeCycleTooMature(LifeCycleState.READY);

        listeningThreads.forEach(thead -> {
            thead.start();
            thead.startListen();
        });
        this.lifecycleState = LifeCycleState.STARTED;
        configuration.startAction();
    }

    /**
     * Pause the listener threads.
     */
    public void pause() {
        checkLifeCycleMatureEnough(LifeCycleState.STARTED);
        checkLifeCycleTooMature(LifeCycleState.STARTED);

        listeningThreads.forEach(UdpServerListeningThread::stopListen);
        configuration.pauseAction();
    }

    /**
     * Resume the listener threads.
     */
    public void resume() {
        checkLifeCycleMatureEnough(LifeCycleState.STARTED);
        checkLifeCycleTooMature(LifeCycleState.STARTED);

        listeningThreads.forEach(UdpServerListeningThread::startListen);
        configuration.resumeAction();
    }

    /**
     * Terminate the server.
     */
    public void terminate() {
        checkLifeCycleMatureEnough(LifeCycleState.STARTED);
        checkLifeCycleTooMature(LifeCycleState.STARTED);

        pause();
        listeningThreads.forEach(UdpServerListeningThread::killListener);
        socket.close();
        configuration.terminationAction();

        this.lifecycleState = LifeCycleState.DEAD;
    }

    /**
     * Handle a message from a client to the server.
     *
     * @param request The request sent to the server
     * @param clientAddress The address of the client who sent the request
     * @param clientPort The port to post back to the client
     */
    public synchronized void handleMessageFromClient(UdpRequest request, InetAddress clientAddress, int clientPort) {
        checkLifeCycleMatureEnough(LifeCycleState.STARTED);
        checkLifeCycleTooMature(LifeCycleState.STARTED);

        // First, check and ensure we do not have a matching client
        final ServerClientCallback sender = clientCallbacks.performRunInLock(clients -> {
            final Optional<ServerClientCallback> callbackOptional = clients.stream().filter(client ->
                client.getAddress().equals(clientAddress) && clientPort == client.getPortNumber()
            ).findFirst();

            ServerClientCallback callback;
            if (callbackOptional.isPresent()) {
                callback = callbackOptional.get();
            } else {
                callback = new ServerClientCallback();
                callback.setAddress(clientAddress);
                callback.setPortNumber(clientPort);
                callback.setKeyId(request.getKeyId());
                clients.add(callback);
            }
            callback.setKeyId(request.getKeyId());
            return callback;
        });

        // We know which client sent the request, now let's have the configuration handle it.
        if (request.getPayloadType() == UdpRequest.PAYLOAD_TYPE_BYTES) {
            configuration.handleBytesMessage(request.getBytePayload(), sender);
        } else {
            configuration.handleStringMessage(request.getStringPayload(), sender);
        }
    }

    /**
     * Broadcast to all clients.
     *
     * @param payload The message
     */
    public void clientBroadcast(String payload) {
        doBroadcast(payload, null);
    }

    /**
     * Broadcast to all clients.
     *
     * @param payload The message
     */
    public void clientBroadcast(byte[] payload) {
        doBroadcast(null, payload);
    }

    /**
     * Perform broadcast to all clients.
     *
     * @param stringPayload Payload in a string format
     * @param bytePayload Payload in a bytes format
     */
    private synchronized void doBroadcast(String stringPayload, byte[] bytePayload) {
        checkLifeCycleMatureEnough(LifeCycleState.STARTED);
        checkLifeCycleTooMature(LifeCycleState.STARTED);

        clientCallbacks.performRunInLock(clients -> {
            clients.forEach(client -> {
                final UdpRequest request = new UdpRequest();
                request.setKeyId(client.getKeyId());
                request.setBytePayload(bytePayload);
                request.setStringPayload(stringPayload);

                if (stringPayload != null) {
                    request.setPayloadType(UdpRequest.PAYLOAD_TYPE_STRING);
                } else {
                    request.setPayloadType(UdpRequest.PAYLOAD_TYPE_BYTES);
                }

                try {
                    final ByteArrayOutputStream byteStream = new ByteArrayOutputStream();
                    final ObjectOutputStream outputStream = new ObjectOutputStream(byteStream);
                    outputStream.writeObject(request);

                    final byte[] payload = byteStream.toByteArray();
                    byteStream.close();

                    final DatagramPacket packet = new DatagramPacket(payload, payload.length, client.getAddress(), client.getPortNumber());
                    socket.send(packet);
                } catch (Exception exception) {
                    // TODO
                    System.out.println("broadcast failed");
                    System.out.println(exception);
                }
            });
        });
    }

    /**
     * Check that the life cycle of the client is enough where operation can be performed.
     *
     * @param state Life-cycle state
     * @throws IllegalStateException If life cycle is too early
     */
    private void checkLifeCycleMatureEnough(LifeCycleState state) {
        if (state.getLevel() > lifecycleState.getLevel()) {
            throw new IllegalStateException("Life cycle step of state " + state + " performed when server is at " + lifecycleState + " state.");
        }
    }

    /**
     * Check that the life cycle of the client has not progressed past where operation can be performed.
     *
     * @param state Life-cycle state
     * @throws IllegalStateException If life cycle is too late
     */
    private void checkLifeCycleTooMature(LifeCycleState state) {
        if (state.getLevel() < lifecycleState.getLevel()) {
            throw new IllegalStateException("Life cycle step of state " + state + " performed when server is at " + lifecycleState + " state.");
        }
    }
}
