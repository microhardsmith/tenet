package cn.zorcc.common.log;

import cn.zorcc.common.WriteBuffer;

@FunctionalInterface
public interface LogHandler {
    void process(WriteBuffer buffer, LogEvent event);
}
