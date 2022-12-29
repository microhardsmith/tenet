package cn.zorcc.log;

import java.util.function.Consumer;

/**
 * 日志事件处理接口
 */
@FunctionalInterface
public interface LogProcessor {
    /**
     * 建立日志事件处理的匿名函数
     */
    Consumer<LogEvent> create();
}
