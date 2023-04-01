package cn.zorcc.common;

import cn.zorcc.common.enums.ExceptionType;
import cn.zorcc.common.exception.FrameworkException;

import java.util.ArrayList;
import java.util.List;

/**
 *   Same as IntMap, but for long
 */
public class LongMap<T> {
    private final int mask;
    private final Entry<T>[] entries;

    @SuppressWarnings("unchecked")
    public LongMap(int size) {
        if(size < 2 || (size & (size - 1)) != 0) {
            throw new FrameworkException(ExceptionType.CONTEXT, "IntMap size must be power of 2");
        }
        this.mask = size - 1;
        this.entries = new Entry[size];
    }


    /**
     *   add element to current map
     */
    public void put(long key, T value) {
        int index = (int) (key & mask);
        Entry<T> current = entries[index];
        Entry<T> newEntry = new Entry<>(key, value);
        if (current == null) {
            entries[index] = newEntry;
        } else {
            while (current.next != null) {
                current = current.next;
            }
            current.next = newEntry;
            newEntry.prev = current;
        }
    }

    /**
     *   get element from current map
     */
    public T get(long key) {
        int index = (int) (key & mask);
        Entry<T> entry = entries[index];
        while (entry != null) {
            if (entry.key == key) {
                return entry.value;
            }
            entry = entry.next;
        }
        return null;
    }

    /**
     *  remove element from current map, return null if not exist
     */
    public T remove(long key) {
        int index = (int) (key & mask);
        Entry<T> current = entries[index];
        while (current != null) {
            if(current.key == key) {
                if (current.prev != null) {
                    current.prev.next = current.next;
                }
                if (current.next != null) {
                    current.next.prev = current.prev;
                }
                if (current == entries[index]) {
                    entries[index] = current.next;
                }
                return current.value;
            }
            current = current.next;
        }
        return null;
    }

    /**
     *   return all the values associated with this map
     */
    public List<T> all() {
        List<T> result = new ArrayList<>();
        for (Entry<T> entry : entries) {
            while (entry != null) {
                result.add(entry.value);
                entry = entry.next;
            }
        }
        return result;
    }

    private static class Entry<T> {
        private final long key;
        private final T value;
        private Entry<T> next;
        private Entry<T> prev;

        private Entry(long key, T value) {
            this.key = key;
            this.value = value;
        }
    }
}
