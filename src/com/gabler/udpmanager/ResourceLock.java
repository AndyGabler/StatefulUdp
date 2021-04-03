package com.gabler.udpmanager;

import java.util.function.Consumer;
import java.util.function.Function;

/**
 * Provider for thread-safe access to a common resource.
 *
 * @author Andy Gabler
 * @param <RESOURCE_TYPE> The type of the resource being locked
 */
public class ResourceLock<RESOURCE_TYPE> {

    private volatile boolean locked = false;
    private volatile RESOURCE_TYPE resource;

    public ResourceLock(RESOURCE_TYPE aResource) {
        this.resource = aResource;
    }

    /**
     * Wait for a lock on the resource and then return it.
     *
     * @return The resource
     */
    public synchronized RESOURCE_TYPE waitForLock() {
        while (locked) {

        }
        locked = true;
        return resource;
    }

    /**
     * Release the lock on the resource.
     */
    public synchronized void releaseLock() {
        locked = false;
    }

    /**
     * Run a block with a safe lock on the resource.
     *
     * @param toRun Block to run
     */
    public synchronized void performRunInLock(Consumer<RESOURCE_TYPE> toRun) {
        toRun.accept(waitForLock());
        releaseLock();
    }

    /**
     * Run a block with a safe lock on the resource.
     *
     * @param toRun Block to run
     * @return Result of the block
     */
    public synchronized <RETURN_TYPE> RETURN_TYPE performRunInLock(Function<RESOURCE_TYPE, RETURN_TYPE> toRun) {
        final RETURN_TYPE result = toRun.apply(waitForLock());
        releaseLock();
        return result;
    }
}
