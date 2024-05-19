package cc.zorcc.core;

import java.lang.foreign.MemorySegment;
import java.lang.foreign.ValueLayout;
import java.lang.invoke.VarHandle;
import java.util.Objects;

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
     *   Get current os type
     */
    public static Os os() {
        return os;
    }

    /**
     *   Checking if target ptr is a NULL pointer
     */
    public static boolean checkNullPtr(MemorySegment ptr) {
        return ptr.isNative() && ptr.address() == 0L;
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
}
