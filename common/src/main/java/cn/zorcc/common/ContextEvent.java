package cn.zorcc.common;

import cn.zorcc.common.enums.ContextEventType;
import cn.zorcc.common.event.Event;
import lombok.Getter;
import lombok.Setter;

/**
 *  应用上下文事件
 */
@Getter
@Setter
public class ContextEvent extends Event {
    /**
     *  上下文事件类型
     */
    private ContextEventType type;
    /**
     *  容器类型
     */
    private Class<?> clazz;
    /**
     *  加载容器
     */
    private Object container;
}
