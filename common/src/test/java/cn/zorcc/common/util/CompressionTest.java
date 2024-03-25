package cn.zorcc.common.util;

import cn.zorcc.common.bindings.DeflateBinding;
import cn.zorcc.common.structure.Allocator;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.lang.foreign.MemorySegment;
import java.lang.foreign.ValueLayout;
import java.nio.charset.StandardCharsets;
import java.util.zip.Deflater;

public class CompressionTest {
    private static final String str = "hello world";
    @Test
    public void testJdkDeflateCompress() {
        byte[] bytes = str.getBytes(StandardCharsets.UTF_8);
        byte[] compressed = CompressUtil.compressUsingJdkDeflate(bytes, Deflater.BEST_SPEED);
        byte[] decompressed = CompressUtil.decompressUsingJdkDeflate(compressed);
        Assertions.assertArrayEquals(bytes, decompressed);
    }

    @Test
    public void testJdkGzipCompress() {
        byte[] bytes = str.getBytes(StandardCharsets.UTF_8);
        byte[] compressed = CompressUtil.compressUsingJdkGzip(bytes, Deflater.BEST_SPEED);
        byte[] decompressed = CompressUtil.decompressUsingJdkGzip(compressed);
        Assertions.assertArrayEquals(bytes, decompressed);
    }

    @Test
    public void testLibDeflateCompress() {
        try(Allocator allocator = Allocator.newDirectAllocator()) {
            byte[] bytes = str.getBytes(StandardCharsets.UTF_8);
            MemorySegment m = MemorySegment.ofArray(str.getBytes(StandardCharsets.UTF_8));
            MemorySegment compressed = CompressUtil.compressUsingDeflate(m, DeflateBinding.LIBDEFLATE_FASTEST_LEVEL, allocator);
            MemorySegment decompressed = CompressUtil.decompressUsingDeflate(compressed, allocator);
            Assertions.assertArrayEquals(bytes, decompressed.toArray(ValueLayout.JAVA_BYTE));
        }
    }

    @Test
    public void testLibGzipCompress() {
        try(Allocator allocator = Allocator.newDirectAllocator()) {
            byte[] bytes = str.getBytes(StandardCharsets.UTF_8);
            MemorySegment m = MemorySegment.ofArray(str.getBytes(StandardCharsets.UTF_8));
            MemorySegment compressed = CompressUtil.compressUsingGzip(m, DeflateBinding.LIBDEFLATE_FASTEST_LEVEL, allocator);
            MemorySegment decompressed = CompressUtil.decompressUsingGzip(compressed, allocator);
            Assertions.assertArrayEquals(bytes, decompressed.toArray(ValueLayout.JAVA_BYTE));
        }
    }
}
