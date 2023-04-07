package cn.zorcc.common.event;

import cn.zorcc.common.enums.ContextEventType;
import lombok.Getter;
import lombok.Setter;

/**
 *  应用上下文事件
 */
@Getter
@Setter
public class ContextEvent {
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
