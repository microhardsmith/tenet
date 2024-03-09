package cn.zorcc.common.structure;

import cn.zorcc.common.Constants;
import cn.zorcc.common.ExceptionType;
import cn.zorcc.common.exception.FrameworkException;
import cn.zorcc.common.util.NativeUtil;

import java.lang.foreign.*;
import java.lang.invoke.MethodHandle;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;

/**
 *   Direct memory ReadBuffer, not thread-safe, ReadBuffer is read-only, shouldn't be modified directly
 *   Note that writeIndex is a mutable field, because
 */
@SuppressWarnings("unused")
public final class ReadBuffer {
    private static final MethodHandle strlen;
    private final MemorySegment segment;
    private final long size;
    private long readIndex;

    static {
        Linker linker = Linker.nativeLinker();
        SymbolLookup symbolLookup = linker.defaultLookup();
        MemorySegment ptr = symbolLookup.find("strnlen").orElseThrow(() -> new FrameworkException(ExceptionType.NATIVE, Constants.UNREACHED));
        strlen = linker.downcallHandle(ptr, FunctionDescriptor.of(ValueLayout.JAVA_LONG, ValueLayout.ADDRESS, ValueLayout.JAVA_LONG), Linker.Option.critical(true));
    }

    public ReadBuffer(MemorySegment segment) {
        this.segment = segment;
        this.size = segment.byteSize();
        this.readIndex = 0L;
    }


    public long readIndex() {
        return readIndex;
    }

    public void setReadIndex(long nextIndex) {
        if(nextIndex < 0L || nextIndex > size) {
            throw new FrameworkException(ExceptionType.NATIVE, "ReadIndex out of bound");
        }
        readIndex = nextIndex;
    }

    public long size() {
        return size;
    }

    public long available() {
        return size - readIndex;
    }

    public byte readByte() {
        long nextIndex = readIndex + Constants.BYTE_SIZE;
        if(nextIndex > size) {
            throw new FrameworkException(ExceptionType.NATIVE, "read index overflow");
        }
        byte b = NativeUtil.getByte(segment, readIndex);
        readIndex = nextIndex;
        return b;
    }

    /**
     *   The returned segment would have the same scope as current ReadBuffer
     */
    public MemorySegment readSegment(long count) {
        long nextIndex = readIndex + count * Constants.BYTE_SIZE;
        if(nextIndex > size) {
            throw new FrameworkException(ExceptionType.NATIVE, "read index overflow");
        }
        MemorySegment result = segment.asSlice(readIndex, count);
        readIndex = nextIndex;
        return result;
    }

    /**
     *   The returning segment would be converted to on-heap memorySegment TODO removal
     */
    public MemorySegment readHeapSegment(long count) {
        MemorySegment m = readSegment(count);
        if(m.isNative()) {
            long len = m.byteSize();
            byte[] bytes = new byte[(int) len];
            MemorySegment h = MemorySegment.ofArray(bytes);
            MemorySegment.copy(m, 0, h, 0, len);
            return h;
        }else {
            return m;
        }
    }

    public byte[] readBytes(long count) {
        return readSegment(count).toArray(ValueLayout.JAVA_BYTE);
    }

    public short readShort() {
        long nextIndex = readIndex + Constants.SHORT_SIZE;
        if(nextIndex > size) {
            throw new FrameworkException(ExceptionType.NATIVE, "read index overflow");
        }
        short s = NativeUtil.getShort(segment, readIndex);
        readIndex = nextIndex;
        return s;
    }

    public int readInt() {
        long nextIndex = readIndex + Constants.INT_SIZE;
        if(nextIndex > size) {
            throw new FrameworkException(ExceptionType.NATIVE, "read index overflow");
        }
        int i = NativeUtil.getInt(segment, readIndex);
        readIndex = nextIndex;
        return i;
    }

    public long readLong() {
        long nextIndex = readIndex + Constants.LONG_SIZE;
        if(nextIndex > size) {
            throw new FrameworkException(ExceptionType.NATIVE, "read index overflow");
        }
        long l = NativeUtil.getLong(segment, readIndex);
        readIndex = nextIndex;
        return l;
    }

    public float readFloat() {
        long nextIndex = readIndex + Constants.FLOAT_SIZE;
        if(nextIndex > size) {
            throw new FrameworkException(ExceptionType.NATIVE, "read index overflow");
        }
        float f = NativeUtil.getFloat(segment, readIndex);
        readIndex = nextIndex;
        return f;
    }

    public double readDouble() {
        long nextIndex = readIndex + Constants.DOUBLE_SIZE;
        if(nextIndex > size) {
            throw new FrameworkException(ExceptionType.NATIVE, "read index overflow");
        }
        double d = NativeUtil.getDouble(segment, readIndex);
        readIndex = nextIndex;
        return d;
    }

    /**
     *   Read until target several separators occurred in the readBuffer
     *   If no separators were found in the sequence, no bytes would be read and null would be returned
     */
    public byte[] readUntil(byte[] separators) {
        for(long cur = readIndex; cur <= size - separators.length; cur++) {
            if(NativeUtil.matches(segment, cur, separators)) {
                byte[] result = cur == readIndex ? Constants.EMPTY_BYTES : segment.asSlice(readIndex, cur - readIndex).toArray(ValueLayout.JAVA_BYTE);
                readIndex = cur + separators.length;
                return result;
            }
        }
        return null;
    }

    public byte[] readUntil(byte b) {
        for(long cur = readIndex; cur < size; cur++) {
            if(segment.get(ValueLayout.JAVA_BYTE, cur) == b) {
                byte[] result = cur == readIndex ? Constants.EMPTY_BYTES : segment.asSlice(readIndex, cur - readIndex).toArray(ValueLayout.JAVA_BYTE);
                readIndex = cur + 1;
                return result;
            }
        }
        return null;
    }

    public byte[] readUntil(byte b1, byte b2) {
        for(long cur = readIndex; cur < size; cur++) {
            if(segment.get(ValueLayout.JAVA_BYTE, cur) == b1 && cur < size - 1 && segment.get(ValueLayout.JAVA_BYTE, cur + 1) == b2) {
                byte[] result = cur == readIndex ? Constants.EMPTY_BYTES : segment.asSlice(readIndex, cur - readIndex).toArray(ValueLayout.JAVA_BYTE);
                readIndex = cur + 2;
                return result;
            }
        }
        return null;
    }

    /**
     *   Read a C style UTF-8 string from the current readBuffer
     */
    public String readStr(Charset charset) {
        long available = size - readIndex;
        long r = strlen(segment.asSlice(readIndex, available), available);
        if(r == available) {
            return null;
        }
        int len = Math.toIntExact(r);
        byte[] bytes = new byte[len];
        MemorySegment.copy(segment, ValueLayout.JAVA_BYTE, readIndex, bytes, 0, len);
        readIndex += len + 1;
        return new String(bytes, charset);
    }

    public String readStr() {
        return readStr(StandardCharsets.UTF_8);
    }


    private static long strlen(MemorySegment ptr, long available) {
        try{
            return (long) strlen.invokeExact(ptr, available);
        }catch (Throwable throwable) {
            throw new FrameworkException(ExceptionType.NATIVE, Constants.UNREACHED);
        }
    }

    /**
     *   Read multiple C style string from current readBuffer, there must be an external NUT to indicate the end
     */
    public List<String> readMultipleStr(Charset charset) {
        List<String> result = new ArrayList<>();
        for( ; ; ) {
            String s = readStr(charset);
            if(s == null) {
                return result;
            }else {
                result.add(s);
            }
        }
    }

    public List<String> readMultipleStr() {
        return readMultipleStr(StandardCharsets.UTF_8);
    }

    @Override
    public String toString() {
        return new String(segment.toArray(ValueLayout.JAVA_BYTE), StandardCharsets.UTF_8);
    }
}
