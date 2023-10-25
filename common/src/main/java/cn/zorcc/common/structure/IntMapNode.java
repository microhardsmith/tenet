package cn.zorcc.common.structure;

/**
 *   Representing a node value in IntMap
 */
public final class IntMapNode<T> {
    private int val;
    private T value;
    private IntMapNode<T> prev;
    private IntMapNode<T> next;

    public int getVal() {
        return val;
    }

    public void setVal(int val) {
        this.val = val;
    }

    public T getValue() {
        return value;
    }

    public void setValue(T value) {
        this.value = value;
    }

    public IntMapNode<T> getPrev() {
        return prev;
    }

    public void setPrev(IntMapNode<T> prev) {
        this.prev = prev;
    }

    public IntMapNode<T> getNext() {
        return next;
    }

    public void setNext(IntMapNode<T> next) {
        this.next = next;
    }
}
