package cn.zorcc.common;

/**
 *   可扩容的byte数组
 */
public class HeapBuffer {
    private final int size;
    private byte[] array;
    private int index;

    public HeapBuffer(int size) {
        this.size = size;
        this.array = new byte[size];
        this.index = 0;
    }

    public void write(byte data) {
        if(index == array.length) {
            resize(index + 1);
        }
        array[index] = data;
        index += 1;
    }

    public void write(byte[] data, int offset, int len) {
        int nextIndex = index + len;
        if(nextIndex > array.length) {
            resize(nextIndex);
        }
        System.arraycopy(data, offset, array, index, len);
        index = nextIndex;
    }

    public void write(byte[] data) {
        write(data, 0, data.length);
    }

    public byte[] array() {
        return array;
    }

    public int index() {
        return index;
    }

    public void reset() {
        index = 0;
        if(array.length > size) {
            array = new byte[size];
        }
    }



    private void resize(int nextIndex) {
        int newSize = Math.max(nextIndex, array.length << 1);
        byte[] newArray = new byte[newSize];
        System.arraycopy(array, 0, newArray, 0, index);
        array = newArray;
    }
}
