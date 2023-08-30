package cn.zorcc.common;

import cn.zorcc.common.anno.Format;

import java.lang.invoke.MethodHandle;
import java.util.Map;
import java.util.function.BiConsumer;
import java.util.function.Function;

public record GtInfo(
        String fieldName,
        Class<?> type,
        MethodHandle gmh,
        MethodHandle smh,
        Function<Object, Object> getter,
        BiConsumer<Object, Object> setter,
        Map<String, Object> enumMap,
        Format format
) {
}
