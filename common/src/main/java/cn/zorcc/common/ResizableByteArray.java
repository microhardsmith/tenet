package cn.zorcc.common;

import java.util.Arrays;

/**
 *   A byte array which is resizable like ArrayList,
 *   This is a replacement for ByteArrayOutputStream, without thread-safety
 */
public final class ResizableByteArray {
    /**
     *   The initial size of the ResizableByteArray will not be changed, after reset the array will fall back to the initialSize
     */
    private final int initialSize;
    private byte[] array;
    private int writeIndex;

    public ResizableByteArray(int initialSize) {
        this.initialSize = initialSize;
        this.array = new byte[initialSize];
        this.writeIndex = 0;
    }

    public void write(byte data) {
        if(writeIndex == array.length) {
            resize(writeIndex + 1);
        }
        array[writeIndex] = data;
        writeIndex += 1;
    }

    public void write(byte[] data, int offset, int len) {
        int nextIndex = writeIndex + len;
        if(nextIndex > array.length) {
            resize(nextIndex);
        }
        System.arraycopy(data, offset, array, writeIndex, len);
        writeIndex = nextIndex;
    }

    public void write(byte[] data) {
        write(data, 0, data.length);
    }

    public byte[] array() {
        return array;
    }

    public byte[] toArray() {
        return Arrays.copyOfRange(array, 0, writeIndex);
    }

    public int writeIndex() {
        return writeIndex;
    }

    public void reset() {
        writeIndex = 0;
        if(array.length > initialSize) {
            array = new byte[initialSize];
        }
    }

    private void resize(int nextIndex) {
        int newSize = Math.max(nextIndex, array.length << 1);
        byte[] newArray = new byte[newSize];
        System.arraycopy(array, 0, newArray, 0, writeIndex);
        array = newArray;
    }
}
