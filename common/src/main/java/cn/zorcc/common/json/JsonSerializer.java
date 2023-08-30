package cn.zorcc.common.json;

import cn.zorcc.common.Writer;

@FunctionalInterface
public interface JsonSerializer<T> {
    void serialize(Writer writer, T t);
}
