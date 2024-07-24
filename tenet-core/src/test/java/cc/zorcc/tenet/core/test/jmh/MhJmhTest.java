package cc.zorcc.tenet.core.test.jmh;

import org.openjdk.jmh.annotations.Benchmark;
import org.openjdk.jmh.annotations.Level;
import org.openjdk.jmh.annotations.OperationsPerInvocation;
import org.openjdk.jmh.annotations.Setup;
import org.openjdk.jmh.infra.Blackhole;

import java.lang.invoke.MethodHandle;
import java.lang.invoke.MethodHandles;
import java.lang.invoke.VarHandle;
import java.util.concurrent.ThreadLocalRandom;

/**
 *   This test focus on the performance gap between methodHandle getter/setters and varHandle getter/setters
 *   It turns out there is no performance difference between them
 */
public class MhJmhTest extends AbstractJmhTest {
    private static final MethodHandle aGetHandle;
    private static final MethodHandle bGetHandle;
    private static final MethodHandle aRefHandle;
    private static final MethodHandle bRefHandle;
    private static final VarHandle aVarHandle;
    private static final VarHandle bVarHandle;

    static {
        try {
            MethodHandles.Lookup lookup = MethodHandles.lookup();
            aGetHandle = lookup.findGetter(A.class, "field", int.class);
            bGetHandle = lookup.findGetter(B.class, "field", int.class);
            aRefHandle = lookup.unreflect(A.class.getDeclaredMethod("getField"));
            bRefHandle = lookup.unreflect(B.class.getDeclaredMethod("field"));
            aVarHandle = lookup.findVarHandle(A.class, "field", int.class);
            bVarHandle = lookup.findVarHandle(B.class, "field", int.class);
        }catch (ReflectiveOperationException e) {
            throw new ExceptionInInitializerError(e);
        }
    }

    private static int aGet(A a) {
        try {
            return (int) aGetHandle.invokeExact(a);
        }catch (Throwable e) {
            throw new RuntimeException(e);
        }
    }

    private static int bGet(B b) {
        try {
            return (int) bGetHandle.invokeExact(b);
        } catch (Throwable e) {
            throw new RuntimeException(e);
        }
    }

    private static int aRef(A a) {
        try {
            return (int) aRefHandle.invokeExact(a);
        } catch (Throwable e) {
            throw new RuntimeException(e);
        }
    }

    private static int bRef(B b) {
        try {
            return (int) bRefHandle.invokeExact(b);
        } catch (Throwable e) {
            throw new RuntimeException(e);
        }
    }

    private static int aVar(A a) {
        return (int) aVarHandle.get(a);
    }

    private static int bVar(B b) {
        return (int) bVarHandle.get(b);
    }

    private A a;
    private B b;
    private static final int times = 1000;

    @Setup(Level.Iteration)
    public void setup() {
        a = new A();
        a.setField(ThreadLocalRandom.current().nextInt());
        b = new B(ThreadLocalRandom.current().nextInt());
    }

    @Benchmark
    @OperationsPerInvocation(times)
    public void testAGet(Blackhole bh) {
        for (int i = 0; i < times; i++) {
            bh.consume(aGet(a));
        }
    }

    @Benchmark
    @OperationsPerInvocation(times)
    public void testARef(Blackhole bh) {
        for (int i = 0; i < times; i++) {
            bh.consume(aRef(a));
        }
    }

    @Benchmark
    @OperationsPerInvocation(times)
    public void testAVar(Blackhole bh) {
        for (int i = 0; i < times; i++) {
            bh.consume(aVar(a));
        }
    }

    @Benchmark
    @OperationsPerInvocation(times)
    public void testBGet(Blackhole bh) {
        for (int i = 0; i < times; i++) {
            bh.consume(bGet(b));
        }
    }

    @Benchmark
    @OperationsPerInvocation(times)
    public void testBRef(Blackhole bh) {
        for (int i = 0; i < times; i++) {
            bh.consume(bRef(b));
        }
    }

    @Benchmark
    @OperationsPerInvocation(times)
    public void testBVar(Blackhole bh) {
        for (int i = 0; i < times; i++) {
            bh.consume(bVar(b));
        }
    }

    /**
     *   Simple bean
     */
    private static final class A {
        private int field;

        public int getField() {
            return field;
        }

        public void setField(int field) {
            this.field = field;
        }
    }

    /**
     *   Simple record
     */
    record B(int field) {

    }

    void main() {
        run(MhJmhTest.class);
    }
}
