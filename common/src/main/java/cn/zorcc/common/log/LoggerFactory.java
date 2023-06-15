package cn.zorcc.common.log;

import org.slf4j.ILoggerFactory;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 *  tenet logger factory
 */
public class LoggerFactory implements ILoggerFactory {
    /**
     *   Logger cache
     */
    private static final Map<String, org.slf4j.Logger> cache = new ConcurrentHashMap<>(1 << 10);
    @Override
    public org.slf4j.Logger getLogger(String name) {
        return cache.computeIfAbsent(name, Logger::new);
    }
}
