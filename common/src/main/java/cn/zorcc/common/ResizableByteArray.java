package cn.zorcc.common;

import cn.zorcc.common.enums.ExceptionType;
import cn.zorcc.common.exception.FrameworkException;

import java.io.IOException;
import java.io.OutputStream;
import java.lang.foreign.MemorySegment;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;

/**
 *   A byte array which is resizable like ArrayList,
 *   This is a replacement for ByteArrayOutputStream, without thread-safety
 */
public final class ResizableByteArray extends OutputStream implements Writer {
    public static final int DEFAULT_SIZE = 32;
    /**
     *   The initial size of the ResizableByteArray will not be changed, after reset the array will fall back to the initialSize
     */
    private final int initialSize;
    private byte[] array;
    private int writeIndex;

    public ResizableByteArray(int initialSize) {
        this.initialSize = initialSize;
        this.array = new byte[initialSize];
        this.writeIndex = Constants.ZERO;
    }

    public ResizableByteArray() {
        this(DEFAULT_SIZE);
    }

    @Override
    public void writeByte(byte data) {
        if(writeIndex == array.length) {
            resize(writeIndex + 1);
        }
        array[writeIndex] = data;
        writeIndex += 1;
    }

    @Override
    public void write(int b) throws IOException {
        writeByte((byte) b);
    }

    @Override
    public void write(byte[] data, int offset, int len) {
        int nextIndex = writeIndex + len;
        if(nextIndex > array.length) {
            resize(nextIndex);
        }
        System.arraycopy(data, offset, array, writeIndex, len);
        writeIndex = nextIndex;
    }

    @Override
    public void close() {
        // No operation needed
    }

    @Override
    public void writeBytes(byte... data) {
        write(data, Constants.ZERO, data.length);
    }

    @Override
    public void writeBytes(byte[] data, int offset, int len) {
        write(data, offset, len);
    }

    @Override
    public String asString() {
        String s = new String(array, Constants.ZERO, writeIndex, StandardCharsets.UTF_8);
        reset();
        return s;
    }

    @Override
    public ReadBuffer asReadBuffer() {
        byte[] b = new byte[writeIndex];
        System.arraycopy(array, Constants.ZERO, b, Constants.ZERO, writeIndex);
        MemorySegment memorySegment = MemorySegment.ofArray(b);
        reset();
        return new ReadBuffer(memorySegment);
    }

    public byte[] rawArray() {
        return array;
    }

    public byte[] toArray() {
        return writeIndex == Constants.ZERO ? Constants.EMPTY_BYTES : Arrays.copyOfRange(array, Constants.ZERO, writeIndex);
    }

    public int writeIndex() {
        return writeIndex;
    }

    public void reset() {
        writeIndex = Constants.ZERO;
        if(array.length > initialSize) {
            array = new byte[initialSize];
        }
    }

    private void resize(int nextIndex) {
        int newSize = Math.max(nextIndex, array.length << 1);
        if(newSize < Constants.ZERO) {
            throw new FrameworkException(ExceptionType.NATIVE, "MemorySize overflow");
        }
        byte[] newArray = new byte[newSize];
        System.arraycopy(array, Constants.ZERO, newArray, Constants.ZERO, writeIndex);
        array = newArray;
    }
}
