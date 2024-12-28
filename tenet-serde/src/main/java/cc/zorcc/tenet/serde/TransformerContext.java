package cc.zorcc.tenet.serde;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.*;
import java.util.concurrent.atomic.AtomicReference;
import java.util.stream.Collectors;

public final class TransformerContext {

    record Entry(
            String name,
            TransformData transformData
    ) {

    }

    private static final AtomicReference<Thread> initializer = new AtomicReference<>();
    private static final Set<Entry> transformerInfos = new HashSet<>();
    private static final Map<String, TransformData> transformerInfoMap;

    static {
        ClassLoader classLoader = TransformerContext.class.getClassLoader();
        try(InputStream stream = classLoader.getResourceAsStream("transform.txt")) {
            if(stream == null) {
                transformerInfoMap = Map.of();
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
                transformerInfoMap = transformerInfos.stream().collect(Collectors.toUnmodifiableMap(Entry::name, Entry::transformData));
            }
        } catch (ReflectiveOperationException | IOException e) {
            throw new ExceptionInInitializerError(e);
        }
    }

    /**
     *   TransformerContext should never be initialized
     */
    private TransformerContext() {
        throw new UnsupportedOperationException();
    }

    /**
     *   Registering transformer, this function should only be invoked by generated classes
     */
    public static void registerTransformer(String name, TransformData transformData) {
        Thread currentThread = Thread.currentThread();
        Thread loaderThread = initializer.getAndSet(currentThread);
        if(loaderThread != null && loaderThread != currentThread) {
            throw new ExceptionInInitializerError("registerTransformer() should only be invoked by TransformerContext");
        }
        Entry entry = new Entry(name, transformData);
        if(transformerInfos.add(entry)) {
            return ;
        }
        throw new SerdeException("Duplicate transformerInfo");
    }

    /**
     *   Obtain the transformerInfo by its name
     */
    public static TransformData getTransformerInfo(String name) {
        return transformerInfoMap.get(name);
    }
}
