package cc.zorcc.tenet.serdeproc.example;

import cc.zorcc.tenet.serde.*;
import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;

import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;

@AimAt(target = "cc.zorcc.tenet.serdeproc.Empty")
public final class EmptyExample implements Refer<Empty> {
    private static final AtomicBoolean initialized = new AtomicBoolean(false);
    private static final EmptyExample SINGLETON = new EmptyExample();

    private EmptyExample() {
        if(!initialized.compareAndSet(false, true)) {
            throw new IllegalStateException("EmptyExample already initialized");
        }
    }

    static {
        SerdeContext.registerRefer(Empty.class, SINGLETON);
    }

    @Override
    public @NonNull List<String> fields() {
        return List.of();
    }

    @Override
    public @NonNull Builder<Empty> builder() {
        return () -> null;
    }

    @Override
    public @Nullable Col<Empty> col(@NonNull String colName) {
        return null;
    }

    @Override
    public @Nullable Empty byName(@NonNull String name) {
        return switch (name) {
            case "T1" -> Empty.T1;
            case "T2" -> Empty.T2;
            case "T3" -> Empty.T3;
            default -> null;
        };
    }
}
