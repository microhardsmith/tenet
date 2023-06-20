package cn.zorcc.common.log;

import cn.zorcc.common.enums.ExceptionType;
import cn.zorcc.common.exception.FrameworkException;
import org.slf4j.ILoggerFactory;
import org.slf4j.IMarkerFactory;
import org.slf4j.helpers.BasicMarkerFactory;
import org.slf4j.helpers.NOPMDCAdapter;
import org.slf4j.spi.MDCAdapter;
import org.slf4j.spi.SLF4JServiceProvider;

import java.util.concurrent.atomic.AtomicBoolean;

/**
 *   ServiceProvider for Tenet log, not supporting Marker or MDC
 */
public final class LogServiceProvider implements SLF4JServiceProvider {
    /**
     *  Slf4j major version 2.0
     */
    private static final String API_VERSION = "2.0.99";
    private static final AtomicBoolean instanceFlag = new AtomicBoolean(false);
    private final ILoggerFactory loggerFactory = new LoggerFactory();
    private final IMarkerFactory markerFactory = new BasicMarkerFactory();
    private final MDCAdapter mdcAdapter = new NOPMDCAdapter();

    @Override
    public ILoggerFactory getLoggerFactory() {
        return loggerFactory;
    }

    @Override
    public IMarkerFactory getMarkerFactory() {
        return markerFactory;
    }

    @Override
    public MDCAdapter getMDCAdapter() {
        return mdcAdapter;
    }

    @Override
    public String getRequestedApiVersion() {
        return API_VERSION;
    }

    @Override
    public void initialize() {
        if(!instanceFlag.compareAndSet(false, true)) {
            throw new FrameworkException(ExceptionType.LOG, "Tenet logServiceProvider has been initialized");
        }
    }
}
