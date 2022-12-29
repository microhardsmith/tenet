package cn.zorcc.common.event;

import cn.zorcc.common.LifeCycle;

/**
 *  事件处理器接口
 */
public interface EventHandler<T extends Event> extends LifeCycle {

    /**
     *  事件处理逻辑
     */
    void handle(T event);
}
