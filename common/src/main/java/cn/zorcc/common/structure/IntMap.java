package cn.zorcc.common.structure;

import cn.zorcc.common.Constants;
import cn.zorcc.common.ExceptionType;
import cn.zorcc.common.exception.FrameworkException;

import java.util.ArrayList;
import java.util.List;

public final class IntMap<T> {
    private final IntMapNode<T>[] nodes;
    private final int mask;
    private int count = 0;

    @SuppressWarnings("unchecked")
    public IntMap(int size) {
        if(Integer.bitCount(size) != 1) {
            throw new FrameworkException(ExceptionType.CONTEXT, Constants.UNREACHED);
        }
        this.mask = size - 1;
        this.nodes = (IntMapNode<T>[]) new IntMapNode[size];
    }

    /**
     *   Get target value from the map, return null if it doesn't exist
     */
    public T get(int val) {
        int slot = val & mask;
        IntMapNode<T> current = nodes[slot];
        while (current != null) {
            if(current.getVal() == val) {
                return current.getValue();
            }else {
                current = current.getNext();
            }
        }
        return null;
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
        count++;
    }

    /**
     *   Replace an existing key-value pair, the old key-value pair must already exist, or an Exception would be thrown
     */
    public void replace(int val, T oldValue, T newValue) {
        int slot = val & mask;
        IntMapNode<T> current = nodes[slot];
        while (current != null) {
            if(current.getVal() == val && current.getValue() == oldValue) {
                current.setValue(newValue);
                return ;
            }else {
                current = current.getNext();
            }
        }
        throw new FrameworkException(ExceptionType.CONTEXT, Constants.UNREACHED);
    }

    /**
     *   Remove target value from current map, return if removed successfully
     */
    public boolean remove(int val, T value) {
        int slot = val & mask;
        IntMapNode<T> current = nodes[slot];
        while (current != null){
            if(current.getVal() != val) {
                current = current.getNext();
            }else if(current.getValue() != value) {
                return false;
            }else {
                IntMapNode<T> prev = current.getPrev();
                IntMapNode<T> next = current.getNext();
                if(prev != null) {
                    prev.setNext(next);
                }else {
                    nodes[slot] = next;
                }
                if(next != null) {
                    next.setPrev(prev);
                }
                current.setPrev(null);
                current.setNext(null);
                count--;
                return true;
            }
        }
        return false;
    }

    /**
     *   Return the element count of current IntMap
     */
    public int count() {
        return count;
    }

    /**
     *   Check if current IntMap is empty
     */
    public boolean isEmpty() {
        return count == 0;
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
