package cc.zorcc.tenet.serde;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicReference;
import java.util.stream.Collectors;

/**
 *   SerdeContext provided access for classes, records, and enums annotated with @Serde, for accessing its constructor, getters and setters
 *   All the refers are initialized when perform classloading, all the generated classes should only be loaded by SerdeContext in its static initialization block
 */
public final class SerdeContext {
    private static final AtomicReference<Thread> initializer = new AtomicReference<>();
    private static final List<Map.Entry<Class<?>, Refer<?>>> classRefers = new ArrayList<>();
    private static final List<Map.Entry<Class<?>, Refer<?>>> recordRefers = new ArrayList<>();
    private static final List<Map.Entry<Class<?>, Refer<?>>> enumRefers = new ArrayList<>();
    private static final Map<Class<?>, Refer<?>> classReferMap;
    private static final Map<Class<?>, Refer<?>> recordReferMap;
    private static final Map<Class<?>, Refer<?>> enumReferMap;

    static {
        ClassLoader classLoader = SerdeContext.class.getClassLoader();
        try(InputStream stream = classLoader.getResourceAsStream("serde.txt")) {
            if(stream == null) {
                classReferMap = Map.of();
                recordReferMap = Map.of();
                enumReferMap = Map.of();
            } else {
                try(BufferedReader reader = new BufferedReader(new InputStreamReader(stream))) {
                    for( ; ; ) {
                        String line = reader.readLine();
                        if(line == null) {
                            break ;
                        }
                        Class.forName(line, true, classLoader); // triggering the class loading mechanism won't require modularized dependencies
                    }
                }
                classReferMap = classRefers.stream().collect(Collectors.toUnmodifiableMap(Map.Entry::getKey, Map.Entry::getValue));
                recordReferMap = recordRefers.stream().collect(Collectors.toUnmodifiableMap(Map.Entry::getKey, Map.Entry::getValue));
                enumReferMap = enumRefers.stream().collect(Collectors.toUnmodifiableMap(Map.Entry::getKey, Map.Entry::getValue));
            }
        } catch (ReflectiveOperationException | IOException e) {
            throw new ExceptionInInitializerError(e);
        }
    }

    /**
     *   Registering refers, this function should only be invoked by generated classes
     */
    public static void registerRefer(Class<?> clazz, Refer<?> refer) {
        Thread currentThread = Thread.currentThread();
        Thread loaderThread = initializer.getAndSet(currentThread);
        if(loaderThread != null && loaderThread != currentThread) {
            throw new ExceptionInInitializerError("registerRefer() should only be invoked by SerdeContext");
        }
        if(clazz.isEnum()) {
            enumRefers.add(Map.entry(clazz, refer));
        }else if(clazz.isRecord()) {
            recordRefers.add(Map.entry(clazz, refer));
        }else {
            classRefers.add(Map.entry(clazz, refer));
        }
    }

    /**
     *   Obtain the target refer by its class type
     */
    @SuppressWarnings("unchecked")
    public static <T> Refer<T> refer(Class<T> clazz) {
        if(clazz.isEnum()) {
            return (Refer<T>) enumReferMap.get(clazz);
        } else if(clazz.isRecord()){
            return (Refer<T>) recordReferMap.get(clazz);
        } else {
            return (Refer<T>) classReferMap.get(clazz);
        }
    }
}
