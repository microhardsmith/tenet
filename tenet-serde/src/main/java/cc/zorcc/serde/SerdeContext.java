package cc.zorcc.serde;

import cc.zorcc.core.FrameworkException;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.lang.invoke.MethodHandles;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;
import java.util.stream.Collectors;

public final class SerdeContext {
    private static final Lock lock = new ReentrantLock();
    private static final List<Map.Entry<Class<?>, Refer<?>>> refers = new ArrayList<>();
    private static final Map<Class<?>, Refer<?>> referMap;

    static {
        try(InputStream stream = SerdeContext.class.getResourceAsStream("/serde.txt")) {
            if(stream == null) {
                referMap = Map.of();
            } else {
                MethodHandles.Lookup lookup = MethodHandles.lookup();
                try(BufferedReader reader = new BufferedReader(new InputStreamReader(stream))) {
                    String className;
                    while ((className = reader.readLine()) != null) {
                        Class<?> c = Class.forName(className);
                        lookup.ensureInitialized(c); // trigger the static initializer
                    }
                }
                referMap = refers.stream().collect(Collectors.toUnmodifiableMap(Map.Entry::getKey, Map.Entry::getValue));
            }
        } catch (Throwable e) {
            throw new ExceptionInInitializerError(e);
        }
    }
}
