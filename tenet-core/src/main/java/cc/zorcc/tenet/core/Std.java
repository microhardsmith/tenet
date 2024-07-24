package cc.zorcc.tenet.core;

import cc.zorcc.tenet.core.bindings.TenetBinding;

import java.lang.foreign.MemorySegment;
import java.lang.foreign.ValueLayout;
import java.lang.invoke.VarHandle;
import java.util.Objects;
import java.util.function.Supplier;

/**
 *   Interaction with non-heap memory and functions
 */
public final class Std {
    /**
     *   Std shouldn't be initialized
     */
    private Std() {
        throw new UnsupportedOperationException();
    }

    /**
     *   Current operating system type
     */
    private static final Os os = currentOs();

    /**
     *   Get current operating system type based from system property
     */
    private static Os currentOs() {
        return switch (Objects.requireNonNull(System.getProperty("os.name")).toLowerCase()) {
            case String s when s.contains("windows") -> Os.Windows;
            case String s when s.contains("linux") -> Os.Linux;
            case String s when s.contains("mac") && s.contains("os") -> Os.macOS;
            default -> Os.Unknown; // Note here null is not reachable
        };
    }

    /**
     *   Whether using vector operations or not
     */
    private static final boolean VECTOR_ENABLED = Boolean.getBoolean("tenet.enableVectorizedOperations");

    /**
     *   Return if current system allow SIMD operations
     */
    public static boolean isVectorEnabled() {
        return VECTOR_ENABLED;
    }

    /**
     *   Get current os type
     */
    public static Os os() {
        return os;
    }

    /**
     *  Get current milliseconds timestamp
     */
    public static long current() {
        return System.currentTimeMillis();
    }

    /**
     *  Get current nanoseconds timestamp
     */
    public static long nano() {
        return System.nanoTime();
    }

    /**
     *  Calculates the nanoseconds elapsed since the specified nanosecond timestamp
     */
    public static long elapsed(long nano) {
        return nano() - nano;
    }

    /**
     * Ensures that the given MemorySegment is non-null, native, and has a valid address.
     * If the MemorySegment is null, non-native, or has an invalid address, an exception is thrown.
     *
     * @param <X> the type of exception to be thrown if the MemorySegment is invalid
     * @param ptr the MemorySegment to be checked; must be non-null, native, and have a non-zero address
     * @param exceptionSupplier a Supplier that provides the exception to be thrown if the MemorySegment is invalid
     * @return the validated MemorySegment if it is non-null, native, and has a valid address
     * @throws X if the MemorySegment is null, non-native, or has an invalid address
     */
    public static <X extends Throwable> MemorySegment requireNonNull(MemorySegment ptr, Supplier<? extends X> exceptionSupplier) throws X {
        if (ptr.isNative() && ptr.address() != 0L) {
            return ptr;
        }else {
            throw exceptionSupplier.get();
        }
    }

    /**
     *   Calculating the next suitable volume for a grown buffer, throw an exception if overflow
     */
    public static int grow(int cap) {
        int newCap = 1 << (Integer.SIZE - Integer.numberOfLeadingZeros(cap));
        if(newCap < 0) {
            throw new ArithmeticException("Capacity overflow");
        }
        return newCap;
    }

    /**
     *   Calculating the next suitable volume for a grown buffer, throw an exception if overflow
     */
    public static long grow(long cap) {
        long newCap = 1L << (Long.SIZE - Long.numberOfLeadingZeros(cap));
        if(newCap < 0L) {
            throw new ArithmeticException("Capacity overflow");
        }
        return newCap;
    }

    /**
     *   Memory accessing methods
     */
    private static final VarHandle BYTE_HANDLE = ValueLayout.JAVA_BYTE.varHandle().withInvokeExactBehavior();
    private static final VarHandle SHORT_HANDLE = ValueLayout.JAVA_SHORT_UNALIGNED.varHandle().withInvokeExactBehavior();
    private static final VarHandle INT_HANDLE = ValueLayout.JAVA_INT_UNALIGNED.varHandle().withInvokeExactBehavior();
    private static final VarHandle LONG_HANDLE = ValueLayout.JAVA_LONG_UNALIGNED.varHandle().withInvokeExactBehavior();
    private static final VarHandle FLOAT_HANDLE = ValueLayout.JAVA_FLOAT_UNALIGNED.varHandle().withInvokeExactBehavior();
    private static final VarHandle DOUBLE_HANDLE = ValueLayout.JAVA_DOUBLE_UNALIGNED.varHandle().withInvokeExactBehavior();
    private static final VarHandle ADDRESS_HANDLE = ValueLayout.ADDRESS_UNALIGNED.varHandle().withInvokeExactBehavior();

    public static byte getByte(MemorySegment m, long offset) {
        return (byte) BYTE_HANDLE.get(m, offset);
    }

    public static void setByte(MemorySegment m, long offset, byte b) {
        BYTE_HANDLE.set(m, offset, b);
    }

    public static short getShort(MemorySegment m, long offset) {
        return (short) SHORT_HANDLE.get(m, offset);
    }

    public static void setShort(MemorySegment m, long offset, short s) {
        SHORT_HANDLE.set(m, offset, s);
    }

    public static int getInt(MemorySegment m, long offset) {
        return (int) INT_HANDLE.get(m, offset);
    }

    public static void setInt(MemorySegment m, long offset, int i) {
        INT_HANDLE.set(m, offset, i);
    }

    public static long getLong(MemorySegment m, long offset) {
        return (long) LONG_HANDLE.get(m, offset);
    }

    public static void setLong(MemorySegment m, long offset, long l) {
        LONG_HANDLE.set(m, offset, l);
    }

    public static float getFloat(MemorySegment m, long offset) {
        return (float) FLOAT_HANDLE.get(m, offset);
    }

    public static void setFloat(MemorySegment m, long offset, float f) {
        FLOAT_HANDLE.set(m, offset, f);
    }

    public static double getDouble(MemorySegment m, long offset) {
        return (double) DOUBLE_HANDLE.get(m, offset);
    }

    public static void setDouble(MemorySegment m, long offset, double d) {
        DOUBLE_HANDLE.set(m, offset, d);
    }

    public static MemorySegment getAddress(MemorySegment m, long offset) {
        return (MemorySegment) ADDRESS_HANDLE.get(m, offset);
    }

    public static void setAddress(MemorySegment m, long offset, MemorySegment address) {
        ADDRESS_HANDLE.set(m, offset, address);
    }

    /**
     *   Below are some file-system operations
     */
    private static final MemorySegment stdout = requireNonNull(TenetBinding.stdout(), () -> new TenetException(ExceptionType.NATIVE, "stdout not found"));
    private static final MemorySegment stderr = requireNonNull(TenetBinding.stderr(), () -> new TenetException(ExceptionType.NATIVE, "stderr not found"));

    /**
     *   Obtain the standard output stream
     */
    public static MemorySegment stdout() {
        return stdout;
    }

    /**
     *   Obtain the standard error stream
     */
    public static MemorySegment stderr() {
        return stderr;
    }

    /**
     *   Normalize file system path, using '/' as the separator
     */
    public static String normalizePath(String path) {
        if (Std.os() == Os.Windows) {
            return path.replace("\\", "/");
        } else {
            return path;
        }
    }
}
