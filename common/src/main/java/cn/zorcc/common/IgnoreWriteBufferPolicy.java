package cn.zorcc.common;

import cn.zorcc.common.enums.ExceptionType;
import cn.zorcc.common.exception.FrameworkException;

import java.lang.foreign.Arena;

/**
 *   Ignore writeBufferPolicy, throw a exception if current WriteBuffer wants to resize
 */
public final class IgnoreWriteBufferPolicy implements WriteBufferPolicy {
    private final Arena arena;

    public IgnoreWriteBufferPolicy(Arena arena) {
        this.arena = arena;
    }

    @Override
    public void resize(WriteBuffer writeBuffer, long nextIndex) {
        throw new FrameworkException(ExceptionType.NATIVE, "Current writeBuffer shouldn't be resized");
    }

    @Override
    public void close(WriteBuffer writeBuffer) {
        arena.close();
    }
}
