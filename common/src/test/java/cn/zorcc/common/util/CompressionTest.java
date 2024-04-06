package cn.zorcc.common.util;

import cn.zorcc.common.bindings.DeflateBinding;
import cn.zorcc.common.structure.Allocator;
import cn.zorcc.common.structure.MemApi;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.lang.foreign.MemorySegment;
import java.lang.foreign.ValueLayout;
import java.nio.charset.StandardCharsets;
import java.util.zip.Deflater;

public class CompressionTest {
    private static final String str = "hello world".repeat(100);
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
        try(Allocator allocator = Allocator.newDirectAllocator(MemApi.DEFAULT)) {
            byte[] bytes = str.getBytes(StandardCharsets.UTF_8);
            MemorySegment m = allocator.allocateFrom(ValueLayout.JAVA_BYTE, bytes);
            MemorySegment compressed = CompressUtil.compressUsingDeflate(m, DeflateBinding.LIBDEFLATE_FASTEST_LEVEL, MemApi.DEFAULT);
            // default size
            MemorySegment decompressed = CompressUtil.decompressUsingDeflate(NativeUtil.toNative(compressed, allocator), MemApi.DEFAULT);
            byte[] decompressedBytes = decompressed.toArray(ValueLayout.JAVA_BYTE);
            Assertions.assertArrayEquals(bytes, decompressedBytes);
            // fixed size
            MemorySegment decompressed2 = CompressUtil.decompressUsingDeflate(NativeUtil.toNative(compressed, allocator), m.byteSize(), MemApi.DEFAULT);
            byte[] decompressedBytes2 = decompressed2.toArray(ValueLayout.JAVA_BYTE);
            Assertions.assertArrayEquals(bytes, decompressedBytes2);
        }
    }

    @Test
    public void testLibGzipCompress() {
        try(Allocator allocator = Allocator.newDirectAllocator(MemApi.DEFAULT)) {
            byte[] bytes = str.getBytes(StandardCharsets.UTF_8);
            MemorySegment m = allocator.allocateFrom(ValueLayout.JAVA_BYTE, bytes);
            MemorySegment compressed = CompressUtil.compressUsingGzip(m, DeflateBinding.LIBDEFLATE_FASTEST_LEVEL, MemApi.DEFAULT);
            // default size
            MemorySegment decompressed = CompressUtil.decompressUsingGzip(NativeUtil.toNative(compressed, allocator), MemApi.DEFAULT);
            byte[] decompressedBytes = decompressed.toArray(ValueLayout.JAVA_BYTE);
            Assertions.assertArrayEquals(bytes, decompressedBytes);
            // fixed size
            MemorySegment decompressed2 = CompressUtil.decompressUsingGzip(NativeUtil.toNative(compressed, allocator), m.byteSize(), MemApi.DEFAULT);
            byte[] decompressedBytes2 = decompressed2.toArray(ValueLayout.JAVA_BYTE);
            Assertions.assertArrayEquals(bytes, decompressedBytes2);
        }
    }

    @Test
    public void testBrotliCompress() {
        try(Allocator allocator = Allocator.newDirectAllocator(MemApi.DEFAULT)) {
            byte[] bytes = str.getBytes(StandardCharsets.UTF_8);
            MemorySegment m = allocator.allocateFrom(ValueLayout.JAVA_BYTE, bytes);
            MemorySegment compressed = CompressUtil.compressUsingBrotli(m, MemApi.DEFAULT);
            // default size
            MemorySegment decompressed = CompressUtil.decompressUsingBrotli(NativeUtil.toNative(compressed, allocator), MemApi.DEFAULT);
            byte[] decompressedBytes = decompressed.toArray(ValueLayout.JAVA_BYTE);
            Assertions.assertArrayEquals(bytes, decompressedBytes);
            // smaller size
            MemorySegment decompressed2 = CompressUtil.decompressUsingBrotli(NativeUtil.toNative(compressed, allocator), compressed.byteSize(), MemApi.DEFAULT);
            byte[] decompressedBytes2 = decompressed2.toArray(ValueLayout.JAVA_BYTE);
            Assertions.assertArrayEquals(bytes, decompressedBytes2);
        }
    }
}
