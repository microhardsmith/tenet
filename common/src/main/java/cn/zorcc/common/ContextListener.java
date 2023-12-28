package cn.zorcc.common;

public interface ContextListener {
    /**
     *   Before the context was initialized, this function would be invoked
     */
    void beforeStarted();

    /**
     *   After a container was loaded, this function would be invoked
     */
    void onLoaded(Object target, Class<?> type);

    /**
     *   After a non-exist container was requested, this function would be invoked
     *   The implementation should return Optional.empty() if no auto-registry could be provided
     */
    <T> T onRequested(Class<T> type);

    /**
     *   After all the container was initialized successfully, this function would be invoked
     */
    void afterStarted();
}
