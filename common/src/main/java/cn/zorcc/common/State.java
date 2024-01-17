package cn.zorcc.common;

import cn.zorcc.common.structure.LongHolder;
import cn.zorcc.common.structure.Mutex;

/**
 *   State represents a long value that could be accessed by multiple thread
 */
public record State(
        Mutex mutex,
        LongHolder holder
) {

    public State(long initialState) {
        this(new Mutex(), new LongHolder(initialState));
    }

    public Mutex withMutex() {
        return mutex.acquire();
    }

    public long get() {
        return holder.getValue();
    }

    public void set(long state) {
        holder.setValue(state);
    }

    public void register(long mask) {
        holder.setValue(holder.getValue() | mask);
    }

    public boolean unregister(long mask) {
        long current = holder.getValue();
        boolean r = (current & mask) != 0L;
        holder.setValue(current & ~mask);
        return r;
    }
}
