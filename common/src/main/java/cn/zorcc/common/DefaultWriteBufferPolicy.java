package cn.zorcc.common;

import cn.zorcc.common.enums.ExceptionType;
import cn.zorcc.common.exception.FrameworkException;

import java.lang.foreign.Arena;
import java.lang.foreign.MemorySegment;
import java.lang.foreign.ValueLayout;

/**
 *   Default writeBufferPolicy, double the memory allocated to contain the grown bytes
 */
public final class DefaultWriteBufferPolicy implements WriteBufferPolicy {
    private final Arena arena;

    public DefaultWriteBufferPolicy(Arena arena) {
        this.arena = arena;
    }

    @Override
    public void resize(WriteBuffer writeBuffer, long nextIndex) {
        long newLen = Math.max(nextIndex, writeBuffer.size() << 1);
        if(newLen < 0) {
            throw new FrameworkException(ExceptionType.NATIVE, "MemorySize overflow");
        }
        MemorySegment oldSegment = writeBuffer.segment();
        MemorySegment newSegment = arena.allocateArray(ValueLayout.JAVA_BYTE, newLen);
        MemorySegment.copy(oldSegment, Constants.ZERO, newSegment, Constants.ZERO, writeBuffer.writeIndex());
        writeBuffer.update(newSegment);
    }

    @Override
    public void close(WriteBuffer writeBuffer) {
        arena.close();
    }
}
