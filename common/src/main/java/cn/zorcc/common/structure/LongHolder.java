package cn.zorcc.common.structure;

public final class LongHolder {
    private long value;

    public LongHolder(long initialValue) {
        this.value = initialValue;
    }

    public LongHolder() {
        this(0L);
    }

    public void setValue(long value) {
        this.value = value;
    }

    public long getValue() {
        return value;
    }

}
