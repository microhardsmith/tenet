package cn.zorcc.common;

import cn.zorcc.common.enums.ExceptionType;
import cn.zorcc.common.exception.FrameworkException;

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
     *   向当前map中添加元素
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
     *   从当前map中获取元素
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
     *   从当前map中移除元素,如果元素不存在则返回null
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
