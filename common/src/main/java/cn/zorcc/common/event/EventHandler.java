package cn.zorcc.common.event;

/**
 *  事件处理器接口
 */
@FunctionalInterface
public interface EventHandler<T> {
    /**
     *  事件处理逻辑
     */
    void handle(T event);
}
