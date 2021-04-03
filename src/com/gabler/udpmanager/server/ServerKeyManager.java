package com.gabler.udpmanager.server;

import java.util.HashMap;

/**
 * Key manager for a server.
 *
 * @author Andy Gabler
 */
public class ServerKeyManager {

    private HashMap<String, byte[]> keyMap = new HashMap<>();

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
