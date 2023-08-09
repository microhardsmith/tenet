package cn.zorcc.common.event;

import cn.zorcc.common.enums.ContextEventType;

/**
 *  应用上下文事件
 */
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

    public ContextEventType type() {
        return type;
    }

    public void setType(ContextEventType type) {
        this.type = type;
    }

    public Class<?> clazz() {
        return clazz;
    }

    public void setClazz(Class<?> clazz) {
        this.clazz = clazz;
    }

    public Object container() {
        return container;
    }

    public void setContainer(Object container) {
        this.container = container;
    }
}
