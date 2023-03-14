package cn.zorcc.common.net;

import cn.zorcc.common.net.win.WinLooper;
import cn.zorcc.common.util.NativeUtil;
import lombok.extern.slf4j.Slf4j;

import java.lang.foreign.*;
import java.lang.invoke.MethodHandle;
import java.lang.invoke.VarHandle;

@Slf4j
public class WinTest {

    private static void test() {
        SymbolLookup symbolLookup = NativeUtil.loadLibraryFromResource("/lib/test.dll");
        MemoryLayout book = MemoryLayout.structLayout(ValueLayout.JAVA_INT.withBitAlignment(32).withName("a"), ValueLayout.JAVA_INT.withBitAlignment(32).withName("b"));
        VarHandle a = book.varHandle(MemoryLayout.PathElement.groupElement("a"));
        VarHandle b = book.varHandle(MemoryLayout.PathElement.groupElement("b"));
        MethodHandle hello = NativeUtil.methodHandle(symbolLookup, "hello", FunctionDescriptor.of(ValueLayout.JAVA_INT.withBitAlignment(32), ValueLayout.ADDRESS.withBitAlignment(64)));
        try{
            MemorySegment segment = MemorySegment.allocateNative(book, SegmentScope.auto());
            segment.fill((byte) 0);
            System.out.println("size : " + segment.byteSize());
            System.out.println("java address : " + segment.address());
            int result = (int) hello.invokeExact(segment);
            System.out.println(result);
            int oa = (int) a.get(segment);
            System.out.println("a = " + oa);
            int ob = (int) b.get(segment);
            System.out.println("b = " + ob);
        } catch (Throwable e) {
            throw new RuntimeException(e);
        }
    }

    public static void main(String[] args) {
        new WinLooper(new NetConfig()).init();
    }
}
