package cn.zorcc.common;

import java.util.function.BiConsumer;
import java.util.function.Function;

public record MetaInfo(
        String name,
        Class<?> type,
        Function<Object, Object> getter,
        BiConsumer<Object, Object> setter
) {
}
