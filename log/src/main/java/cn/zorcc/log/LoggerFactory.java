package cn.zorcc.log;

import org.slf4j.ILoggerFactory;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * logger工厂类
 */
public class LoggerFactory implements ILoggerFactory {
    /**
     *   Logger类缓存,每个类应该只使用一个固定的Logger
     */
    private static final Map<String, org.slf4j.Logger> cache = new ConcurrentHashMap<>(1 << 10);
    @Override
    public org.slf4j.Logger getLogger(String name) {
        return cache.computeIfAbsent(name, Logger::new);
    }
}
