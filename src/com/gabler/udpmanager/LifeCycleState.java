package com.gabler.udpmanager;

/**
 * State in the life-cycle of either a client or server.
 *
 * @author Andy Gabler
 */
public enum LifeCycleState {

    INITIALIZED(0),
    READY(1),
    STARTED(2),
    DEAD(3);

    int level;

    public int getLevel() {
        return level;
    }

    LifeCycleState(int aLevel) {
        level = aLevel;
    }
}
