package com.gabler.udpmanager.server;

import com.gabler.udpmanager.LifeCycleState;
import com.gabler.udpmanager.ResourceLock;
import com.gabler.udpmanager.model.UdpRequest;
import com.gabler.udpmanager.security.AesBytesToCiphertextTransformer;
import com.gabler.udpmanager.security.AesCiphertextToBytesTransformer;

import java.io.ByteArrayOutputStream;
import java.io.ObjectOutputStream;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.SocketException;
import java.util.ArrayList;
import java.util.Base64;
import java.util.Optional;
import java.util.function.BiFunction;
import java.util.function.Function;

/**
 * Abstraction for the management of a UDP socket acting as a server.
 *
 * @author Andy Gabler
 */
public class UdpServer {

    private IUdpServerConfiguration configuration;
    private volatile LifeCycleState lifecycleState;

    private final BiFunction<byte[], byte[], byte[]> aesBytesToCiphertextTransformer;
    private final BiFunction<byte[], byte[], byte[]> aesCipherTextToBytesTransformer;
    private final DatagramSocket socket;
    private final ArrayList<UdpServerListeningThread> listeningThreads;
    private final ResourceLock<ArrayList<ServerClientCallback>> clientCallbacks;
    private final ResourceLock<ServerKeyManager> keyManager;

    /**
     * Initialize an abstraction
     *
     * @param portNumber The server's port
     * @param threadPoolSize Amount of listener threads to spin off
     * @throws SocketException If the port cannot be used
     */
    public UdpServer(int portNumber, int threadPoolSize) throws SocketException {
        this(
            portNumber,
            threadPoolSize,
            new AesBytesToCiphertextTransformer(),
            new AesCiphertextToBytesTransformer()
        );
    }

    /**
     * Initialize an abstraction
     *
     * @param portNumber The server's port
     * @param threadPoolSize Amount of listener threads to spin off
     * @param anAesBytesToCiphertextTransformer Encryption manager
     * @param anAesCiphertextToBytesTransformer Decryption manager
     * @throws SocketException If the port cannot be used
     */
    public UdpServer(
        int portNumber,
        int threadPoolSize,
        BiFunction<byte[], byte[], byte[]> anAesBytesToCiphertextTransformer,
        BiFunction<byte[], byte[], byte[]> anAesCiphertextToBytesTransformer
    ) throws SocketException {
        this.lifecycleState = LifeCycleState.INITIALIZED;
        socket = new DatagramSocket(portNumber);
        listeningThreads = new ArrayList<>();
        clientCallbacks = new ResourceLock<>(new ArrayList<>());
        keyManager = new ResourceLock<>(new ServerKeyManager());

        /*
         * It is possible for multiple threads to receive from the same DatagramSocket, but only one of them will get
         * each packet. Create listening threads.
         */
        for (int counter = 0; threadPoolSize > counter; counter++) {
            listeningThreads.add(new UdpServerListeningThread(this, socket, counter));
        }

        aesBytesToCiphertextTransformer = anAesBytesToCiphertextTransformer;
        aesCipherTextToBytesTransformer = anAesCiphertextToBytesTransformer;
    }

    /**
     * Add an identifier on the for a cryptographic key used to encrypt payloads.
     *
     * It is recommended to cycle the keys every now and then since IV is not used.
     *
     * @param id The key id
     */
    public synchronized void addClientKey(String id, byte[] key) {
        keyManager.performRunInLock(manager -> {
            manager.addKey(id, key);
        });
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

            /*
             * It is recommended to cycle the keys every now and then since IV is not used.
             * Therefore, if the client requests a new key be used, so be it.
             */
            callback.setKeyId(request.getKeyId());
            return callback;
        });

        // We know which client sent the request, now let's have the configuration handle it.
        if (request.getPayloadType() == UdpRequest.PAYLOAD_TYPE_BYTES) {
            if (sender.getKeyId() != null) {
                keyManager.performRunInLock(manager -> {
                    final byte[] plainText = aesCipherTextToBytesTransformer.apply(request.getBytePayload(), manager.keyForId(sender.getKeyId()));
                    configuration.handleBytesMessage(plainText, sender);
                });
            } else {
                configuration.handleBytesMessage(request.getBytePayload(), sender);
            }
        } else {
            if (sender.getKeyId() != null) {
                keyManager.performRunInLock(manager -> {
                    final byte[] cipherText = Base64.getDecoder().decode(request.getStringPayload());
                    final byte[] plainTextBytes = aesCipherTextToBytesTransformer.apply(cipherText, manager.keyForId(sender.getKeyId()));
                    final String plainText = new String(plainTextBytes);
                    configuration.handleStringMessage(plainText, sender);
                });
            } else {
                configuration.handleStringMessage(request.getStringPayload(), sender);
            }
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

                final byte[] clientKey = keyManager.performRunInLock((Function<ServerKeyManager, byte[]>) manager -> manager.keyForId(client.getKeyId()));
                if (request.getKeyId() != null) {
                    if (bytePayload != null) {
                        final byte[] cipherText = aesBytesToCiphertextTransformer.apply(bytePayload, clientKey);
                        request.setBytePayload(cipherText);
                    } else if (stringPayload != null) {
                        final byte[] stringAsBytes = stringPayload.getBytes();
                        final byte[] cipherTextBytes = aesBytesToCiphertextTransformer.apply(stringAsBytes, clientKey);
                        final String cipherText = Base64.getEncoder().encodeToString(cipherTextBytes);
                        request.setStringPayload(cipherText);
                    }
                } else {
                    request.setBytePayload(bytePayload);
                    request.setStringPayload(stringPayload);
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
                    throw new RuntimeException(exception);
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
