package cn.zorcc.common;

import java.lang.foreign.*;
import java.lang.invoke.MethodHandle;
import java.lang.invoke.VarHandle;

public class TestNative {
    public static void main(String[] args) {
        SymbolLookup symbolLookup = NativeHelper.loadLibraryFromResource("/test.dll");
        MemoryLayout book = MemoryLayout.structLayout(ValueLayout.JAVA_INT.withName("a"), ValueLayout.JAVA_INT.withName("b"));
        VarHandle a = book.varHandle(MemoryLayout.PathElement.groupElement("a"));
        VarHandle b = book.varHandle(MemoryLayout.PathElement.groupElement("b"));
        MethodHandle hello = NativeHelper.methodHandle(symbolLookup, "hello", FunctionDescriptor.of(ValueLayout.JAVA_INT, ValueLayout.ADDRESS));
        try{
            MemorySegment segment = MemorySegment.allocateNative(book, SegmentScope.auto());
            segment.fill((byte) 0);
            int result = (int) hello.invokeExact(segment);
            System.out.println(result);
            System.out.println("a = " + a.get(book));
            System.out.println("b = " + b.get(book));
        } catch (Throwable e) {
            throw new RuntimeException(e);
        }
    }
}
