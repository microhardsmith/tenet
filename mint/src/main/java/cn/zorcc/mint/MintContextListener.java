package cn.zorcc.mint;

import cn.zorcc.common.ContextListener;

import java.util.Optional;

public final class MintContextListener implements ContextListener {
    @Override
    public void onLoaded(Object target, Class<?> type) {
        // TODO
    }

    @Override
    public <T> Optional<T> onRequested(Class<T> type) {
        // TODO
        return Optional.empty();
    }

    @Override
    public void onStarted() {
        // TODO
    }
}
