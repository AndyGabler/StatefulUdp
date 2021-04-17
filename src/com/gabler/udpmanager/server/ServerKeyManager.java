package com.gabler.udpmanager.server;

import java.util.concurrent.ConcurrentHashMap;

/**
 * Key manager for a server.
 *
 * @author Andy Gabler
 */
public class ServerKeyManager {

    /*
     * Concurrent hash map of the keys.
     * This has thread-safe reads with no locks.
     * It has a write lock (which doesn't block reads!).
     * The only consequence is if the key server is being overloaded with requests and completing them all at the same
     * time.
     */
    private ConcurrentHashMap<String, byte[]> keyMap = new ConcurrentHashMap<>();

    /**
     * Add a key to the manager.
     *
     * @param id Identifier for the key
     * @param key The key
     */
    public void addKey(String id, byte[] key) {
        keyMap.put(id, key);
    }

    /**
     * Get a key for the key's id.
     *
     * @param id The id
     * @return The key
     */
    public byte[] keyForId(String id) {
        if (id == null) {
            return null;
        }

        final byte[] key = keyMap.get(id);

        if (key == null) {
            throw new IllegalArgumentException("No key for ID " + id);
        }
        return key;
    }
}
