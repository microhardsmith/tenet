package cn.zorcc.common;

import cn.zorcc.common.enums.ExceptionType;
import cn.zorcc.common.exception.FrameworkException;

import java.lang.foreign.Arena;
import java.lang.foreign.MemorySegment;
import java.lang.foreign.ValueLayout;

/**
 *   Reserved writeBufferPolicy, the initial memorySegment will be reserved and never released, following resize operation is the same as DefaultWriteBufferPolicy
 */
public final class ReservedWriteBufferPolicy implements WriteBufferPolicy {
    private Arena arena = null;

    @Override
    public void resize(WriteBuffer writeBuffer, long nextIndex) {
        long newLen = Math.max(nextIndex, writeBuffer.size() << 1);
        if(newLen < 0) {
            throw new FrameworkException(ExceptionType.NATIVE, "MemorySize overflow");
        }
        if(arena == null) {
            arena = Arena.ofConfined();
        }
        MemorySegment oldSegment = writeBuffer.segment();
        MemorySegment newSegment = arena.allocateArray(ValueLayout.JAVA_BYTE, newLen);
        MemorySegment.copy(oldSegment, Constants.ZERO, newSegment, Constants.ZERO, writeBuffer.writeIndex());
        writeBuffer.update(newSegment);
    }

    @Override
    public void close(WriteBuffer writeBuffer) {
        if(arena != null) {
            arena.close();
        }else {
            writeBuffer.setWriteIndex(Constants.ZERO);
        }
    }
}
