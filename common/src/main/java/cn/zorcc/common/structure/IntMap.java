package cn.zorcc.common.structure;

import cn.zorcc.common.Constants;
import cn.zorcc.common.enums.ExceptionType;
import cn.zorcc.common.exception.FrameworkException;

import java.util.ArrayList;
import java.util.List;

public final class IntMap<T> {
    private final Node<T>[] nodes;
    private final int mask;

    @SuppressWarnings("unchecked")
    public IntMap(int size) {
        this.mask = size - Constants.ONE;
        if((size & mask) != Constants.ZERO) {
            throw new FrameworkException(ExceptionType.NETWORK, "size must be power of 2");
        }
        this.nodes = (Node<T>[]) new Node[size];
        for(int index = Constants.ZERO; index < size; index++) {
            Node<T> header = new Node<>();
            header.val = Constants.ZERO;
            nodes[index] = header;
        }
    }

    /**
     *   Get target value from the map, return null if it doesn't exist
     */
    public T get(int val) {
        int slot = val & mask;
        Node<T> header = nodes[slot];
        Node<T> current = header.next;
        while (current != null) {
            if(current.val == val) {
                return current.value;
            }else {
                current = current.next;
            }
        }
        return null;
    }

    /**
     *   Insert a new object into the map, the value must not exist in the map before, or there will be unknown mistakes
     */
    public void put(int val, T value) {
        Node<T> n = new Node<>();
        n.val = val;
        n.value = value;
        int slot = val & mask;
        Node<T> header = nodes[slot];
        Node<T> next = header.next;
        header.next = n;
        n.prev = header;
        if(next != null) {
            n.next = next;
            next.prev = n;
        }
        header.val += Constants.ONE;
    }

    /**
     *   Replace a existing key-value pair, the old key-value pair must already exist, or there will be unknown mistakes
     */
    public void replace(int val, T value) {
        int slot = val & mask;
        Node<T> header = nodes[slot];
        Node<T> current = header.next;
        while (current != null) {
            if(current.val == val) {
                current.value = value;
                return ;
            }else {
                current = current.next;
            }
        }
        throw new FrameworkException(ExceptionType.NETWORK, Constants.UNREACHED);
    }

    /**
     *   Remove target actor from current map, return if removed successfully
     */
    public boolean remove(int val, T value) {
        int slot = val & mask;
        Node<T> header = nodes[slot];
        Node<T> current = header.next;
        for( ; ; ) {
            if(current == null) {
                return false;
            }
            if(current.val != val) {
                current = current.next;
            }else if(current.value != value) {
                return false;
            }else {
                Node<T> prev = current.prev;
                Node<T> next = current.next;
                prev.next = next;
                if(next != null) {
                    next.prev = prev;
                }
                current.prev = null;
                current.next = null;
                header.val -= Constants.ONE;
                return true;
            }
        }
    }

    public List<T> toList() {
        List<T> result = new ArrayList<>();
        for (Node<T> header : nodes) {
            int len = header.val;
            Node<T> current = header.next;
            for(int i = Constants.ZERO; i < len; i++) {
                result.add(current.value);
                current = current.next;
            }
        }
        return result;
    }

    private static class Node<T> {
        private int val;
        private T value;
        private Node<T> prev;
        private Node<T> next;
    }
}
