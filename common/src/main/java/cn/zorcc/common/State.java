package cn.zorcc.common;

import cn.zorcc.common.structure.Mutex;

/**
 *   State represents an int that could be accessed by multiple thread
 */
public final class State {
    private final Mutex mutex = new Mutex();
    private int state;

    public State() {
        this(Constants.INITIAL);
    }

    public State(int initialState) {
        this.state = initialState;
    }

    public Mutex withMutex() {
        return mutex.acquire();
    }

    public int get() {
        return state;
    }

    public void set(int state) {
        this.state = state;
    }

    public void register(int mask) {
        this.state |= mask;
    }

    public boolean unregister(int mask) {
        boolean r = (state & mask) > 0;
        state &= ~mask;
        return r;
    }
}
