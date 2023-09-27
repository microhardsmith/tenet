package cn.zorcc.common.util;

import cn.zorcc.common.Constants;

import java.lang.foreign.MemorySegment;
import java.util.function.Consumer;

public final class StringUtil {
    private StringUtil() {
        throw new UnsupportedOperationException();
    }

    /**
     *   Find next expected byte in the data from startIndex, return the target index + 1 of the expected byte
     */
    public static long searchBytes(MemorySegment data, byte expected, long startIndex, Consumer<MemorySegment> consumer) {
        for(long index = startIndex; index < data.byteSize(); index++) {
            if(NativeUtil.getByte(data, index) == expected) {
                if(index > startIndex) {
                    consumer.accept(data.asSlice(startIndex, index - startIndex));
                }
                return index + Constants.ONE == data.byteSize() ? Long.MIN_VALUE : index + Constants.ONE;
            }
        }
        return Long.MIN_VALUE;
    }
}
