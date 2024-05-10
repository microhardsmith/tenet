package cn.zorcc.common.serde;

import cn.zorcc.common.Constants;
import cn.zorcc.common.ExceptionType;
import cn.zorcc.common.exception.FrameworkException;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.lang.invoke.MethodHandles;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;
import java.util.stream.Collectors;

public final class SerdeContext {
    private static final Lock lock = new ReentrantLock();
    private static final List<Map.Entry<Class<?>, Handle<?>>> entryList = new ArrayList<>();
    private static final Map<Class<?>, Handle<?>> handleMap;

    static {
        try(InputStream stream = SerdeContext.class.getResourceAsStream("/serde.txt")) {
            if(stream == null) {
                handleMap = Map.of();
            } else {
                MethodHandles.Lookup lookup = MethodHandles.lookup();
                try(BufferedReader reader = new BufferedReader(new InputStreamReader(stream))) {
                    String className;
                    while ((className = reader.readLine()) != null) {
                        Class<?> c = Class.forName(className);
                        lookup.ensureInitialized(c); // trigger the static initializer
                    }
                }
                handleMap = entryList.stream().collect(Collectors.toUnmodifiableMap(Map.Entry::getKey, Map.Entry::getValue));
            }
        } catch (Throwable e) {
            throw new FrameworkException(ExceptionType.CONTEXT, Constants.UNREACHED, e);
        }
    }

    /**
     *   This function shouldn't be renamed, it will be used by generated source
     */
    @SuppressWarnings("unused")
    public static <T> void registerHandle(Class<T> clazz, Handle<T> handle) {
        lock.lock();
        try {
            entryList.add(Map.entry(clazz, handle));
        } finally {
            lock.unlock();
        }
    }

    /**
     *   Obtaining a handle from current SerdeContext, if the target clazz was not annotated with @Serde, then a NPE would be thrown
     */
    @SuppressWarnings("unchecked")
    public static <T> Handle<T> getHandle(Class<T> clazz) {
        return (Handle<T>) Objects.requireNonNull(handleMap.get(clazz));
    }
}
