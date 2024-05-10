package cn.zorcc.common.serde;

import cn.zorcc.common.Constants;
import cn.zorcc.common.ExceptionType;
import cn.zorcc.common.exception.FrameworkException;

import java.lang.invoke.MethodHandles;
import java.util.AbstractMap;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

public final class GenerationContext {
    private static final List<Map.Entry<Class<?>, Handle<?>>> entryList = new ArrayList<>();
    private static final Map<Class<?>, Handle<?>> handleMap;

    static {
        try{
            MethodHandles.Lookup lookup = MethodHandles.lookup();
            lookup.ensureInitialized(Class.forName("cn.zorcc.common.serde.BeanGenerationExample"));
            lookup.ensureInitialized(Class.forName("cn.zorcc.common.serde.EnumGenerationExample"));
            lookup.ensureInitialized(Class.forName("cn.zorcc.common.serde.RecordGenerationExample"));
            handleMap = entryList.stream().collect(Collectors.toUnmodifiableMap(Map.Entry::getKey, Map.Entry::getValue));
        } catch (Throwable e) {
            throw new FrameworkException(ExceptionType.CONTEXT, Constants.UNREACHED, e);
        }
    }

    public static <T> void registerHandle(final Class<T> clazz, final Handle<T> handle) {
        entryList.add(new AbstractMap.SimpleImmutableEntry<>(clazz, handle));
    }

    @SuppressWarnings("unchecked")
    public static <T> Handle<T> getHandle(final Class<T> clazz) {
        return (Handle<T>) handleMap.get(clazz);
    }
}
