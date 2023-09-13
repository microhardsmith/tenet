package cn.zorcc.common;

import cn.zorcc.common.anno.Format;

import java.lang.reflect.Type;
import java.util.function.Function;

public record RecordInfo(
        String fieldName,
        Class<?> fieldClass,
        Type genericType,
        Function<Object, Object> getter,
        Format format
) {
}
