package cn.zorcc.common.access;

public interface Getter<T> extends Iterable<Object> {
    Object getValue(String key);
}
