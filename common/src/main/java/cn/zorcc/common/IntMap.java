package cn.zorcc.common;

import cn.zorcc.common.enums.ExceptionType;
import cn.zorcc.common.exception.FrameworkException;

import java.util.ArrayList;
import java.util.List;

/**
 *   用于key值固定为int类型的Map,非线程安全，固定不进行扩容
 *   使用者需要保证key值的唯一性，map不会对已有值进行覆写
 */
public class IntMap<T> {
    private final int mask;
    private final Entry<T>[] entries;

    @SuppressWarnings("unchecked")
    public IntMap(int size) {
        if(size < 2 || (size & (size - 1)) != 0) {
            throw new FrameworkException(ExceptionType.CONTEXT, "IntMap size must be power of 2");
        }
        this.mask = size - 1;
        this.entries = new Entry[size];
    }

    /**
     *   add element to current map
     */
    public void put(int key, T value) {
        int index = key & mask;
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
    public T get(int key) {
        int index = key & mask;
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
    public T remove(int key) {
        int index = key & mask;
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
        private final int key;
        private final T value;
        private Entry<T> next;
        private Entry<T> prev;

        private Entry(int key, T value) {
            this.key = key;
            this.value = value;
        }
    }
}
