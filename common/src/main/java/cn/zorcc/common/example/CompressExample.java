package cn.zorcc.common.example;

import cn.zorcc.common.util.CompressUtil;

import java.lang.foreign.MemorySegment;
import java.lang.foreign.ValueLayout;
import java.nio.charset.StandardCharsets;

public class CompressExample {
    private static final byte[] data = "hello".repeat(10000).getBytes(StandardCharsets.UTF_8);

    public static void main(String[] args) {
        MemorySegment m1 = CompressUtil.compressUsingDeflate(MemorySegment.ofArray(data), CompressUtil.FASTEST_LEVEL);
        MemorySegment m2 = CompressUtil.decompressUsingDeflate(m1);
        System.out.println(new String(m2.toArray(ValueLayout.JAVA_BYTE), StandardCharsets.UTF_8));
    }
}
