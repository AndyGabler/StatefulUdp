package com.gabler.udpmanager.server;

import java.net.InetAddress;
import java.util.Optional;
import java.util.concurrent.CopyOnWriteArrayList;

/**
 * Manager for client connections to the server.
 *
 * @author Andy Gabler
 */
public class ServerClientManager {

    private final CopyOnWriteArrayList<ServerClientCallback> callbacks = new CopyOnWriteArrayList<>();

    /**
     * Return all clients.
     *
     * @return All clients
     */
    public CopyOnWriteArrayList<ServerClientCallback> getAll() {
        return callbacks;
    }

    /**
     * Get client for address and port number. Ensures manager knows about client.
     *
     * @param clientAddress The address of the client
     * @param portNumber The port number of the client
     * @param keyId The key the client wishes to use
     * @return The client callback
     */
    public ServerClientCallback getForAddressAndPort(InetAddress clientAddress, int portNumber, String keyId) {
        final Optional<ServerClientCallback> callbackOptional = callbacks.stream().filter(client ->
            client.getAddress().equals(clientAddress) && portNumber == client.getPortNumber()
        ).findFirst();

        ServerClientCallback callback;
        if (callbackOptional.isPresent()) {
            callback = callbackOptional.get();
        } else {
            callback = new ServerClientCallback();
            callback.setAddress(clientAddress);
            callback.setPortNumber(portNumber);
            callback.setKeyId(keyId);
            callbacks.add(callback);
        }

        /*
         * It is recommended to cycle the keys every now and then since IV is not used.
         * Therefore, if the client requests a new key be used, so be it.
         */
        callback.setKeyId(keyId);
        return callback;
    }
}
