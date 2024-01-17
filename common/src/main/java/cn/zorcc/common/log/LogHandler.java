package cn.zorcc.common.log;

import cn.zorcc.common.structure.WriteBuffer;

@FunctionalInterface
public interface LogHandler {
    void process(WriteBuffer buffer, LogEvent event);
}
