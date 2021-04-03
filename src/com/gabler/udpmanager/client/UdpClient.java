package com.gabler.udpmanager.client;

import com.gabler.udpmanager.LifeCycleState;
import com.gabler.udpmanager.ResourceLock;
import com.gabler.udpmanager.model.UdpRequest;
import com.gabler.udpmanager.security.AesBytesToCiphertextTransformer;
import com.gabler.udpmanager.security.AesCiphertextToBytesTransformer;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.ObjectOutputStream;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.SocketException;
import java.net.UnknownHostException;
import java.util.Base64;
import java.util.function.BiFunction;

/**
 * Abstraction for the management of a UDP socket acting as a client.
 *
 * @author Andy Gabler
 */
public class UdpClient {

    private final BiFunction<byte[], byte[], byte[]> aesBytesToCiphertextTransformer;
    private final BiFunction<byte[], byte[], byte[]> aesCipherTextToBytesTransformer;

    private final InetAddress address;
    private final int portNumber;
    private final DatagramSocket socket;
    private final ResourceLock<ClientKey> clientKey;

    private IUdpClientConfiguration configuration;
    private volatile LifeCycleState lifecycleState;

    private volatile UdpClientListeningThread listeningThread = null;

    /**
     * Initialize an abstraction for the management of a UDP socket acting as a client.
     *
     * @param host The name of the host
     * @param aPortNumber The port number
     * @throws UnknownHostException If the host is not found
     * @throws SocketException If socket cannot be made
     */
    public UdpClient(String host, int aPortNumber) throws UnknownHostException, SocketException {
        this(
            host,
            aPortNumber,
            new AesBytesToCiphertextTransformer(),
            new AesCiphertextToBytesTransformer()
        );
    }

    /**
     * Initialize an abstraction for the management of a UDP socket acting as a client.
     *
     * @param host The name of the host
     * @param aPortNumber The port number
     * @param anAesBytesToCiphertextTransformer Encryption manager
     * @param anAesCiphertextToBytesTransformer Decryption manager
     * @throws UnknownHostException If the host is not found
     * @throws SocketException If socket cannot be made
     */
    public UdpClient(
        String host,
        int aPortNumber,
        BiFunction<byte[], byte[], byte[]> anAesBytesToCiphertextTransformer,
        BiFunction<byte[], byte[], byte[]> anAesCiphertextToBytesTransformer
    ) throws UnknownHostException, SocketException {
        this.lifecycleState = LifeCycleState.INITIALIZED;
        address = InetAddress.getByName(host);
        portNumber = aPortNumber;
        socket = new DatagramSocket();
        clientKey = new ResourceLock<>(new ClientKey());
        aesBytesToCiphertextTransformer = anAesBytesToCiphertextTransformer;
        aesCipherTextToBytesTransformer = anAesCiphertextToBytesTransformer;
    }

    /**
     * Set the identifier on the server's side for the cryptographic key used to encrypt payloads.
     *
     * It is recommended to cycle the keys every now and then since IV is not used.
     *
     * @param id The key id
     * @param key The key to use
     */
    public synchronized void setClientKey(String id, byte[] key) {
        clientKey.performRunInLock(clientSideKey -> {
            clientSideKey.setKeyId(id);
            clientSideKey.setKeyBytes(key);
        });
    }

    /**
     * Set the client configuration being used.
     *
     * @param configuration The configuration.
     */
    public void setConfiguration(IUdpClientConfiguration configuration) {
        checkLifeCycleTooMature(LifeCycleState.READY);
        this.configuration = configuration;
        lifecycleState = LifeCycleState.READY;
    }

    /**
     * Start the client.
     */
    public void start() {
        checkLifeCycleMatureEnough(LifeCycleState.READY);
        checkLifeCycleTooMature(LifeCycleState.READY);

        // Ensure we connect, creates a listening post-back address.
        socket.connect(address, portNumber);

        // Setup the listening thread
        listeningThread = new UdpClientListeningThread(this, this.socket);
        listeningThread.start();
        listeningThread.startListen();

        this.lifecycleState = LifeCycleState.STARTED;
        configuration.startAction();
    }

    /**
     * Pause the listener thread.
     */
    public void pause() {
        checkLifeCycleMatureEnough(LifeCycleState.STARTED);
        checkLifeCycleTooMature(LifeCycleState.STARTED);

        listeningThread.stopListen();
        configuration.pauseAction();
    }

    /**
     * Resume the listener thread from pause.
     */
    public void resume() {
        checkLifeCycleMatureEnough(LifeCycleState.STARTED);
        checkLifeCycleTooMature(LifeCycleState.STARTED);

        listeningThread.startListen();
        configuration.resumeAction();
    }

    /**
     * Terminate the client.
     */
    public void terminate() {
        checkLifeCycleMatureEnough(LifeCycleState.STARTED);
        checkLifeCycleTooMature(LifeCycleState.STARTED);

        pause();
        listeningThread.killListener();
        socket.close();
        configuration.terminationAction();

        lifecycleState = LifeCycleState.DEAD;
    }

    /**
     * Handle a message from the server.
     *
     * @param request The UDP request which contains a payload
     */
    public synchronized void handleMessageFromServer(UdpRequest request) {
        checkLifeCycleMatureEnough(LifeCycleState.STARTED);
        checkLifeCycleTooMature(LifeCycleState.STARTED);

        if (request.getPayloadType() == UdpRequest.PAYLOAD_TYPE_BYTES) {
            clientKey.performRunInLock(key -> {
                if (key.getKeyBytes() != null) {
                    final byte[] plainText = aesCipherTextToBytesTransformer.apply(request.getBytePayload(), key.getKeyBytes());
                    configuration.handleBytesMessage(plainText);
                } else {
                    configuration.handleBytesMessage(request.getBytePayload());
                }
            });
        } else {
            clientKey.performRunInLock(key -> {
                if (key.getKeyBytes() != null) {
                    final byte[] cipherText = Base64.getDecoder().decode(request.getStringPayload());
                    final byte[] plainTextBytes = aesCipherTextToBytesTransformer.apply(cipherText, key.getKeyBytes());
                    final String plainText = new String(plainTextBytes);
                    configuration.handleStringMessage(plainText);
                } else {
                    configuration.handleStringMessage(request.getStringPayload());
                }
            });
        }
    }

    /**
     * Send a message to the server.
     *
     * @param payload The message to send
     * @throws IOException If send fails
     */
    public void sendMessageToServer(String payload) throws IOException {
        sendToServer(payload, null);
    }

    /**
     * Send a message to the server.
     *
     * @param payload The message to send
     * @throws IOException If send fails
     */
    public void sendMessageToServer(byte[] payload) throws IOException {
        sendToServer(null, payload);
    }

    /**
     * Perform the actual sending of a message to the server.
     *
     * @param stringPayload Payload in a string format
     * @param bytePayload Payload in a bytes format
     * @throws IOException If the send fails
     */
    private synchronized void sendToServer(String stringPayload, byte[] bytePayload) throws IOException {
        checkLifeCycleMatureEnough(LifeCycleState.STARTED);
        checkLifeCycleTooMature(LifeCycleState.STARTED);

        final UdpRequest request = new UdpRequest();
        request.setKeyId(clientKey.waitForLock().getKeyId());
        clientKey.releaseLock();

        if (request.getKeyId() != null) {
            if (bytePayload != null) {
                final byte[] cipherText = aesBytesToCiphertextTransformer.apply(bytePayload, clientKey.waitForLock().getKeyBytes());
                request.setBytePayload(cipherText);
                clientKey.releaseLock();
            } else if (stringPayload != null) {
                final byte[] stringAsBytes = stringPayload.getBytes();
                final byte[] cipherTextBytes = aesBytesToCiphertextTransformer.apply(stringAsBytes, clientKey.waitForLock().getKeyBytes());
                clientKey.releaseLock();
                final String cipherText = Base64.getEncoder().encodeToString(cipherTextBytes);
                request.setStringPayload(cipherText);
            }
        } else {
            request.setBytePayload(bytePayload);
            request.setStringPayload(stringPayload);
        }

        if (stringPayload != null) {
            request.setPayloadType(UdpRequest.PAYLOAD_TYPE_STRING);
        } else {
            request.setPayloadType(UdpRequest.PAYLOAD_TYPE_BYTES);
        }

        final ByteArrayOutputStream byteStream = new ByteArrayOutputStream();
        final ObjectOutputStream outputStream = new ObjectOutputStream(byteStream);
        outputStream.writeObject(request);

        final byte[] payload = byteStream.toByteArray();
        byteStream.close();

        final DatagramPacket packet = new DatagramPacket(payload, payload.length, address, portNumber);
        socket.send(packet);
    }

    /**
     * Check that the life cycle of the client is enough where operation can be performed.
     *
     * @param state Life-cycle state
     * @throws IllegalStateException If life cycle is too early
     */
    private void checkLifeCycleMatureEnough(LifeCycleState state) {
        if (state.getLevel() > lifecycleState.getLevel()) {
            throw new IllegalStateException("Life cycle step of state " + state + " performed when client is at " + lifecycleState + " state.");
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
            throw new IllegalStateException("Life cycle step of state " + state + " performed when client is at " + lifecycleState + " state.");
        }
    }
}
