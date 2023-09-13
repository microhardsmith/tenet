package cn.zorcc.common;

import cn.zorcc.common.anno.Format;

import java.util.function.Function;

public record RecordInfo(
        String fieldName,
        Function<Object, Object> getter,
        Format format
) {
}
