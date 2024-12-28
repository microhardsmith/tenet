package cc.zorcc.tenet.serde;

import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.*;
import java.util.concurrent.atomic.AtomicReference;
import java.util.stream.Collectors;

/**
 *   SerdeContext provided access for classes, records, and enums annotated with @Serde, for accessing its constructor, getters and setters
 *   All the refers are initialized when perform classloading, all the generated classes should only be loaded by SerdeContext in its static initialization block
 */
public final class SerdeContext {

    record Entry(
            Class<?> type,
            Refer<?> refer
    ) {
        @Override
        public boolean equals(Object obj) {
            return obj instanceof Entry entry && type.equals(entry.type);
        }
    }

    private static final AtomicReference<Thread> initializer = new AtomicReference<>();
    private static final Set<Entry> classRefers = new HashSet<>();
    private static final Set<Entry> recordRefers = new HashSet<>();
    private static final Set<Entry> enumRefers = new HashSet<>();
    private static final Map<Class<?>, Refer<?>> classReferMap;
    private static final Map<Class<?>, Refer<?>> recordReferMap;
    private static final Map<Class<?>, Refer<?>> enumReferMap;

    static {
        ClassLoader classLoader = SerdeContext.class.getClassLoader();
        try(InputStream stream = classLoader.getResourceAsStream("serde.txt")) {
            if(stream != null) {
                try(BufferedReader reader = new BufferedReader(new InputStreamReader(stream))) {
                    for( ; ; ) {
                        String line = reader.readLine();
                        if(line == null) {
                            break ;
                        }
                        Class.forName(line, true, classLoader); // triggering the class loading mechanism won't require modularized dependencies
                    }
                }
            }
            classReferMap = classRefers.stream().collect(Collectors.toUnmodifiableMap(Entry::type, Entry::refer));
            recordReferMap = recordRefers.stream().collect(Collectors.toUnmodifiableMap(Entry::type, Entry::refer));
            enumReferMap = enumRefers.stream().collect(Collectors.toUnmodifiableMap(Entry::type, Entry::refer));
        } catch (ReflectiveOperationException | IOException e) {
            throw new ExceptionInInitializerError(e);
        }
    }

    /**
     *   SerdeContext should never be initialized
     */
    private SerdeContext() {
        throw new UnsupportedOperationException();
    }

    /**
     *   Registering refers, this function should only be invoked by generated classes
     */
    public static void registerRefer(@NonNull Class<?> clazz, @NonNull Refer<?> refer) {
        Thread currentThread = Thread.currentThread();
        Thread loaderThread = initializer.getAndSet(currentThread);
        if(loaderThread != null && loaderThread != currentThread) {
            throw new ExceptionInInitializerError("registerRefer() should only be invoked by SerdeContext");
        }
        Entry entry = new Entry(clazz, refer);
        if((clazz.isEnum() && enumRefers.add(entry)) || (clazz.isRecord() && recordRefers.add(entry)) || classRefers.add(entry)) {
            return ;
        }
        throw new SerdeException("Duplicate refers");
    }

    /**
     *   Obtain the target refer by its class type
     */
    @SuppressWarnings("unchecked")
    public static <T> @Nullable Refer<T> refer(@NonNull Class<T> clazz) {
        if(clazz.isEnum()) {
            return (Refer<T>) enumReferMap.get(clazz);
        } else if(clazz.isRecord()){
            return (Refer<T>) recordReferMap.get(clazz);
        } else {
            return (Refer<T>) classReferMap.get(clazz);
        }
    }
}
