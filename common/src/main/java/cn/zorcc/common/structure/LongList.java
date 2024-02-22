package cn.zorcc.common.structure;

import java.util.Arrays;
import java.util.function.LongConsumer;

/**
 *   LongList is an array of long to avoid box and unboxing
 */
public final class LongList {
    private static final int DEFAULT_CAPACITY = 10;
    private long[] data;
    private int size;

    public LongList() {
        this(DEFAULT_CAPACITY);
    }

    public LongList(int size) {
        this.data = new long[size];
        this.size = 0;
    }

    public void add(long value) {
        ensureCapacity();
        data[size++] = value;
    }

    public long get(int index) {
        if (index < 0 || index >= size) {
            throw new IndexOutOfBoundsException(STR."Index: \{index}, Size: \{size}");
        }
        return data[index];
    }

    public int size() {
        return size;
    }

    public void forEach(LongConsumer consumer) {
        for (long l : data) {
            consumer.accept(l);
        }
    }

    private void ensureCapacity() {
        int len = data.length;
        if (size == len) {
            int newCapacity = len + (len >> 1);
            data = Arrays.copyOf(data, newCapacity);
        }
    }
}
