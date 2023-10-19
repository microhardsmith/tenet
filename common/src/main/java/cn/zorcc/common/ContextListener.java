package cn.zorcc.common;

import java.util.Optional;

public interface ContextListener {
    /**
     *   After a container was loaded, this function would be invoked
     */
    void onLoaded(Object target, Class<?> type);

    /**
     *   After a non-exist container was requested, this function would be invoked
     *   The implementation should return Optional.empty() if no auto-registry could be provided
     */
    <T> Optional<T> onRequested(Class<T> type);

    /**
     *   After all the container was initialized successfully, this function would be invoked
     */
    void onStarted();
}
