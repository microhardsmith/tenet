package cn.zorcc.common;

import cn.zorcc.common.enums.ContextEventType;
import cn.zorcc.common.enums.ExceptionType;
import cn.zorcc.common.event.Event;
import cn.zorcc.common.event.EventPipeline;
import cn.zorcc.common.exception.FrameworkException;
import cn.zorcc.common.pojo.Peer;
import lombok.extern.slf4j.Slf4j;

import java.lang.management.ManagementFactory;
import java.lang.management.RuntimeMXBean;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 *  应用上下文
 */
@Slf4j
public class Context {
    /**
     *  初始化标识
     */
    private static final AtomicBoolean initFlag = new AtomicBoolean(false);
    /**
     *  容器管理
     */
    private static final Map<Class<?>, Object> containerMap = new ConcurrentHashMap<>();
    /**
     *  运行时自动生成的容器管理
     */
    private static final Map<Class<?>, Object> autoMap = new ConcurrentHashMap<>();
    /**
     *  事件pipeline管理
     */
    private static final Map<Class<? extends Event>, EventPipeline<? extends Event>> pipelineMap = new ConcurrentHashMap<>();
    /**
     *  当前节点信息
     */
    private static Peer self;

    /**
     *  初始化应用Context
     */
    public static void init(Peer peer) {
        if (!initFlag.compareAndSet(false, true)) {
            throw new FrameworkException(ExceptionType.CONTEXT, "Context has been initialized");
        }
        self = peer;
        long nano = Clock.nano();
        RuntimeMXBean runtimeMXBean = ManagementFactory.getRuntimeMXBean();
        String[] pidAndDevice = runtimeMXBean.getName().split("@");
        log.info("Process running with Pid: {} on Device: {}", pidAndDevice[0], pidAndDevice[1]);
    }

    public static Peer self() {
        return self;
    }

    /**
     * 注册指定类型的事件pipeline,如果已存在则抛出异常
     * @param clazz 事件类
     * @param pipeline 事件对应pipeline对象
     */
    public static <T extends Event> void registerPipeline(Class<T> clazz, EventPipeline<T> pipeline) {
        if (!Objects.equals(pipelineMap.putIfAbsent(clazz, pipeline), pipeline)) {
            throw new FrameworkException(ExceptionType.CONTEXT, "Pipeline already exist for %s".formatted(clazz.getSimpleName()));
        }
    }

    /**
     *  获取指定类型的事件pipeline,如果不存在则返回null
     * @param clazz 事件类
     * @return 事件对应pipeline对象
     */
    @SuppressWarnings("unchecked")
    public static <T extends Event> EventPipeline<T> pipeline(Class<T> clazz) {
        return (EventPipeline<T>) pipelineMap.get(clazz);
    }

    /**
     * 注册容器,注册成功之后触发ContextEvent Load事件
     * @param obj 容器对象
     * @param clazz 容器类型
     */
    public static <T> void loadContainer(T obj, Class<T> clazz) {
        if(obj == null) {
            throw new FrameworkException(ExceptionType.CONTEXT, "Container can't be null");
        }
        if (!clazz.isAssignableFrom(obj.getClass())) {
            throw new FrameworkException(ExceptionType.CONTEXT, "Container doesn't match its class : %s".formatted(clazz.getSimpleName()));
        }
        if(!Objects.equals(containerMap.putIfAbsent(clazz, obj), obj)) {
            throw new FrameworkException(ExceptionType.CONTEXT, "Container already exist : %s".formatted(clazz.getSimpleName()));
        }
        ContextEvent contextEvent = new ContextEvent();
        contextEvent.setType(ContextEventType.Load);
        contextEvent.setClazz(clazz);
        contextEvent.setContainer(obj);
        pipeline(ContextEvent.class).fireEvent(contextEvent);
    }

    /**
     * 获取容器,获取失败触发ContextEvent Get事件,可用于在运行中自动生成容器,无法自动生成时返回null
     * @param clazz 容器类
     * @return 容器对象
     */
    @SuppressWarnings("unchecked")
    public static <T> T getContainer(Class<T> clazz) {
        return (T) containerMap.computeIfAbsent(clazz, k -> {
            ContextEvent contextEvent = new ContextEvent();
            contextEvent.setType(ContextEventType.Get);
            contextEvent.setClazz(clazz);
            pipeline(ContextEvent.class).fireEvent(contextEvent);
            return autoMap.get(clazz);
        });
    }

    /**
     * 自动创建容器对象
     * @param obj 自动生成的容器对象
     * @param clazz 容器类
     */
    public static <T> void create(T obj, Class<T> clazz) {
        if(obj != null && clazz.isAssignableFrom(obj.getClass())) {
            autoMap.putIfAbsent(clazz, obj);
        }
    }

}
