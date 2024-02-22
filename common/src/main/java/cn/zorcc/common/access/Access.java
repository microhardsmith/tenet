package cn.zorcc.common.access;

public interface Access<T> {
    Setter<T> createSetter();

    Getter<T> createGetter();
}
