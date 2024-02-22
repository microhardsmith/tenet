package cn.zorcc.common.access;

public interface Setter<T> {
    void setValue(String key, Object value);

    T construct();
}
