package cn.zorcc.common;

import java.lang.invoke.MethodHandle;
import java.util.Map;
import java.util.function.BiConsumer;
import java.util.function.Function;

public record MetaInfo(
        String name,
        Class<?> type,
        MethodHandle gmh,
        MethodHandle smh,
        Function<Object, Object> getter,
        BiConsumer<Object, Object> setter,
        Map<String, Object> enumMap
) {
}
