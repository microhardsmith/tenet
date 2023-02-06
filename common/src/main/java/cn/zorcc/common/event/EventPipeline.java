package cn.zorcc.common.event;

import cn.zorcc.common.LifeCycle;
import cn.zorcc.common.enums.ExceptionType;
import cn.zorcc.common.exception.FrameworkException;

import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 *  事件链式处理,非线程安全,在触发事件之前需要确定事件链的处理过程
 */
public class EventPipeline<T extends Event> implements LifeCycle {
    private final AtomicBoolean initFlag = new AtomicBoolean(false);
    private final EventHandler<T>[] eventHandlers;

    @SuppressWarnings("unchecked")
    public EventPipeline(List<EventHandler<T>> eventHandlers) {
        int size = eventHandlers.size();
        this.eventHandlers = (EventHandler<T>[]) new EventHandler[size];
        for(int i = 0; i < size; i++) {
            this.eventHandlers[i] = eventHandlers.get(i);
        }
    }

    /**
     *  初始化pipeline,依次初始化pipeline上的所有EventHandler之后,启动消费者线程
     */
    @Override
    public void init() {
        if (!initFlag.compareAndSet(false, true)) {
            throw new FrameworkException(ExceptionType.CONTEXT, "EventPipeline already in initialization");
        }
        for (EventHandler<T> eventHandler : eventHandlers) {
            eventHandler.init();
        }
    }


    /**
     *  主动触发事件,事件在init之前并不会被处理
     *  尽量在新建的虚拟线程中触发事件，事件的触发是默认同步的
     */
    public void fireEvent(T event) {
        for (EventHandler<T> eventHandler : eventHandlers) {
            eventHandler.handle(event);
        }
    }

    /**
     *  销毁事件链上的所有EventHandler,不保证当前EventQueue中的事件仍会被消费
     */
    @Override
    public void shutdown() {
        for (EventHandler<T> eventHandler : eventHandlers) {
            eventHandler.shutdown();
        }
    }
}
