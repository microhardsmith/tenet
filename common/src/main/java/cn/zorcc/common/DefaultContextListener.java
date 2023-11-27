package cn.zorcc.common;

import cn.zorcc.common.log.Logger;
import cn.zorcc.common.log.LoggerConsumer;
import cn.zorcc.common.structure.Wheel;

/**
 *   Default implementation for ContextListener, used for test-purpose only
 */
public final class DefaultContextListener implements ContextListener {
    private static final Logger log = new Logger(DefaultContextListener.class);

    @Override
    public void beforeStarted() {
        Context.load(Wheel.wheel(), Wheel.class);
        Context.load(new LoggerConsumer(), LoggerConsumer.class);
    }

    @Override
    public void onLoaded(Object target, Class<?> type) {
        log.debug(STR."Container loaded for \{type.getName()}");
    }

    @Override
    public <T> T onRequested(Class<T> type) {
        return null;
    }

    @Override
    public void onStarted() {
        log.debug("DefaultContext was initialized successfully");
    }
}
