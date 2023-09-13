package cn.zorcc.common;

import cn.zorcc.common.anno.Format;

import java.lang.reflect.Type;
import java.util.function.BiConsumer;
import java.util.function.Function;

public record MetaInfo(
        String fieldName,
        Class<?> fieldClass,
        Type genericType,
        Function<Object, Object> getter,
        BiConsumer<Object, Object> setter,
        Format format
) {

    public Object invokeGetter(Object target) {
        return getter.apply(target);
    }

    public void invokeSetter(Object target, Object field) {
        setter.accept(target, field);
    }
}
