package cn.zorcc.common.structure;

public final class IntHolder {
    private int value;

    public IntHolder(int initialValue) {
        this.value = initialValue;
    }

    public IntHolder() {
        this(0);
    }

    public int getValue() {
        return value;
    }

    public void setValue(int value) {
        this.value = value;
    }

}
