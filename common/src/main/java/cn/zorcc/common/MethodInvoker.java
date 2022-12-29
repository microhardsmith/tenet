package cn.zorcc.common;

import com.esotericsoftware.reflectasm.MethodAccess;
import lombok.extern.slf4j.Slf4j;

import java.lang.reflect.Method;
import java.util.*;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

/**
 *  本地反射方法调用处理器,通过Reflectasm进行注册调用比直接反射性能更好
 *  该类是并发安全的,全局共享单例
 */
@Slf4j
public enum MethodInvoker {
    INSTANCE;
    /**
     *  反射调用信息缓存
     */
    private final Map<Class<?>, MethodAccess> methodAccessMap = new HashMap<>();
    /**
     *  Invoker列表
     */
    private final List<Invoker> invokerList = new ArrayList<>();
    /**
     *  并发控制访问
     */
    private final Lock lock = new ReentrantLock();

    /**
     *  反射调用相关信息
     */
    record Invoker(Object impl, MethodAccess methodAccess, int methodIndex) {
        public Object invoke(Object[] args) {
            return methodAccess.invoke(impl, methodIndex, args);
        }
    }

    /**
     * 注册可进行反射调用的方法
     * @param impl 方法对象
     * @param method 需要被注册的方法
     * @return 被注册的方法调用索引
     */
    public int registerMethodMapping(Object impl, Method method) {
        lock.lock();
        try{
            int index = invokerList.size();
            log.info("Registering MethodMapping for method : {}, index : {}", method.getName(), index);
            Class<?> implClass = impl.getClass();
            MethodAccess methodAccess = methodAccessMap.computeIfAbsent(implClass, MethodAccess::get);
            int methodIndex = methodAccess.getIndex(method.getName(), method.getParameterTypes());
            invokerList.add(new Invoker(impl, methodAccess, methodIndex));
            return index;
        }finally {
            lock.unlock();
        }
    }

    /**
     * 触发方法调用
     * @param methodIndex 方法索引
     * @param args 参数列表
     * @return 方法调用的返回值
     */
    public Object invoke(int methodIndex, Object[] args) {
        Invoker invoker = invokerList.get(methodIndex);
        return invoker == null ? null : invoker.invoke(args);
    }
}
