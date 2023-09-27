package cn.zorcc.common;

import cn.zorcc.common.log.Logger;

public final class DefaultContextListener implements ContextListener {
    private static final Logger log = new Logger(DefaultContextListener.class);
    @Override
    public void onLoaded(Object target, Class<?> type) {
        log.debug(STR."Container loaded for \{type.getName()}");
    }

    @Override
    public Object onRequested(Class<?> type) {
        return null;
    }
}
