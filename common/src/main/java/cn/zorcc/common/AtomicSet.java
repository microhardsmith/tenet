package cn.zorcc.common;

import java.util.Collections;
import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.atomic.AtomicReference;

/**
 *  原子更新的Set
 */
public class AtomicSet<T> {
    private final AtomicReference<Set<T>> reference;

    public AtomicSet() {
        this.reference = new AtomicReference<>(new HashSet<>());
    }

    /**
     *  向Set中添加元素，返回添加之后的Set集合
     */
    public Set<T> add(T t) {
        return reference.updateAndGet(set -> {
            Set<T> next = new HashSet<>(set);
            next.add(t);
            return Collections.unmodifiableSet(next);
        });
    }

    /**
     *  向Set中移除元素，返回添加之后的Set集合
     */
    public Set<T> remove(T t) {
        return reference.updateAndGet(set -> {
            Set<T> next = new HashSet<>(set);
            next.remove(t);
            return Collections.unmodifiableSet(next);
        });
    }

    /**
     *  获取当前Set对象，获取到的是一个不可变Set，不允许对其进行修改
     */
    public Set<T> get() {
        return reference.get();
    }
}
