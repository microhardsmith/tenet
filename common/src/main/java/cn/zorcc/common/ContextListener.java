package cn.zorcc.common;

public interface ContextListener {
    void onLoaded(Object target, Class<?> type);

    Object onRequested(Class<?> type);
}
