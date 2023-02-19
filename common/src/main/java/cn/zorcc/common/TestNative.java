package cn.zorcc.common;

import java.lang.foreign.*;
import java.lang.invoke.MethodHandle;

public class TestNative {
    private final MethodHandle methodHandle;
    private final MemorySegment memorySegment;
    private final String nat = "\033[34mnative\033[0m";

    public TestNative() {
        SymbolLookup symbolLookup = NativeHelper.loadLibraryFromResource("/test.dll");
        this.methodHandle = NativeHelper.methodHandle(symbolLookup, "pr", FunctionDescriptor.of(ValueLayout.JAVA_INT, ValueLayout.ADDRESS));
        this.memorySegment = MemorySegment.allocateNative(1024, SegmentScope.global());
    }
    public static void main(String[] args) throws Throwable {
        TestNative testNative = new TestNative();
        for(int i = 0;i < 1000;i++) {
            testNative.testNative(i);
        }
        Thread.sleep(5000);
    }

    public void testNative(int i) throws Throwable {
        memorySegment.setUtf8String(0, nat + i);
        int r = (int) methodHandle.invokeExact(memorySegment);
    }
}
