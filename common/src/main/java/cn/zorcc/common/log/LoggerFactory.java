package cn.zorcc.common.log;

import cn.zorcc.common.Constants;
import org.slf4j.ILoggerFactory;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 *  Tenet logger factory
 */
public final class LoggerFactory implements ILoggerFactory {
    /**
     *   Logger cache
     */
    private static final Map<String, Logger> cache = new ConcurrentHashMap<>(Constants.KB);
    @Override
    public org.slf4j.Logger getLogger(String name) {
        return cache.computeIfAbsent(name, Logger::new);
    }
}
