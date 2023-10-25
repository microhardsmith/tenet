package cn.zorcc.common.structure;

import cn.zorcc.common.Constants;
import cn.zorcc.common.enums.ExceptionType;
import cn.zorcc.common.exception.FrameworkException;

import java.util.ArrayList;
import java.util.List;

public final class IntMap<T> {
    private final IntMapNode<T>[] nodes;
    private final int mask;

    @SuppressWarnings("unchecked")
    public IntMap(int size) {
        if(Integer.bitCount(size) != 1) {
            throw new FrameworkException(ExceptionType.CONTEXT, Constants.UNREACHED);
        }
        this.mask = size - 1;
        this.nodes = (IntMapNode<T>[]) new IntMapNode[size];
    }

    /**
     *   Get target node from the map, return null if it doesn't exist
     */
    public IntMapNode<T> getNode(int val) {
        int slot = val & mask;
        IntMapNode<T> current = nodes[slot];
        while (current != null) {
            if(current.getVal() == val) {
                return current;
            }else {
                current = current.getNext();
            }
        }
        return null;
    }

    /**
     *   Get target value from the map, return null if it doesn't exist
     */
    public T get(int val) {
        IntMapNode<T> node = getNode(val);
        return node == null ? null : node.getValue();
    }

    /**
     *   Insert a new object into the map, the value must not exist in the map before, or there will be unknown mistakes
     */
    public void put(int val, T value) {
        IntMapNode<T> n = new IntMapNode<>();
        n.setVal(val);
        n.setValue(value);
        int slot = val & mask;
        IntMapNode<T> current = nodes[slot];
        if (current != null) {
            n.setNext(current);
            current.setPrev(n);
        }
        nodes[slot] = n;
    }

    /**
     *   Replace an existing key-value pair, the old key-value pair must already exist, or an Exception would be thrown
     */
    public void replace(int val, T value) {
        int slot = val & mask;
        IntMapNode<T> current = nodes[slot];
        while (current != null) {
            if(current.getVal() == val) {
                current.setValue(value);
                return ;
            }else {
                current = current.getNext();
            }
        }
        throw new FrameworkException(ExceptionType.CONTEXT, Constants.UNREACHED);
    }

    /**
     *   Remove target node from current map, the node must already exist in the map
     */
    public void removeNode(int val, IntMapNode<T> node) {
        int slot = val & mask;
        IntMapNode<T> prev = node.getPrev();
        IntMapNode<T> next = node.getNext();
        if(prev == null) {
            if(node == nodes[slot]) {
                nodes[slot] = next;
                next.setPrev(null);
            }else {
                throw new FrameworkException(ExceptionType.CONTEXT, Constants.UNREACHED);
            }
        }else {
            prev.setNext(next);
            if(next != null) {
                next.setPrev(prev);
            }
            node.setPrev(null);
            node.setNext(null);
        }
    }

    /**
     *   Remove target value from current map, return if removed successfully
     */
    public boolean remove(int val, T value) {
        int slot = val & mask;
        IntMapNode<T> current = nodes[slot];
        for( ; ; ) {
            if(current == null) {
                return false;
            }
            if(current.getVal() != val) {
                current = current.getNext();
            }else if(current.getValue() != value) {
                return false;
            }else {
                IntMapNode<T> prev = current.getPrev();
                IntMapNode<T> next = current.getNext();
                prev.setNext(next);
                if(next != null) {
                    next.setPrev(prev);
                }
                current.setPrev(null);
                current.setNext(null);
                return true;
            }
        }
    }

    public void remove(int val) {
        int slot = val & mask;
        IntMapNode<T> current = nodes[slot];
        for( ; ; ) {
            if(current == null) {
                throw new FrameworkException(ExceptionType.CONTEXT, Constants.UNREACHED);
            }else if(current.getVal() == val) {
                removeNode(val, current);
                return ;
            }else {
                current = current.getNext();
            }
        }
    }

    /**
     *   Convert current IntMap to a list
     */
    public List<T> asList() {
        List<T> result = new ArrayList<>();
        for (IntMapNode<T> n : nodes) {
            IntMapNode<T> t = n;
            while (t != null) {
                result.add(t.getValue());
                t = t.getNext();
            }
        }
        return result;
    }
}
