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
            // default size
            CompressUtil.compressUsingDeflate(m, DeflateBinding.LIBDEFLATE_FASTEST_LEVEL, MemApi.DEFAULT, compressed -> {
                CompressUtil.decompressUsingDeflate(compressed, MemApi.DEFAULT, decompressed -> {
                    byte[] decompressedBytes = decompressed.toArray(ValueLayout.JAVA_BYTE);
                    Assertions.assertArrayEquals(bytes, decompressedBytes);
                });
            });

            // fixed size
            CompressUtil.compressUsingDeflate(m, DeflateBinding.LIBDEFLATE_FASTEST_LEVEL, MemApi.DEFAULT, compressed -> {
                CompressUtil.decompressUsingDeflate(compressed, m.byteSize(), MemApi.DEFAULT, decompressed -> {
                    byte[] decompressedBytes = decompressed.toArray(ValueLayout.JAVA_BYTE);
                    Assertions.assertArrayEquals(bytes, decompressedBytes);
                });
            });
        }
    }

    @Test
    public void testLibGzipCompress() {
        try(Allocator allocator = Allocator.newDirectAllocator(MemApi.DEFAULT)) {
            byte[] bytes = str.getBytes(StandardCharsets.UTF_8);
            MemorySegment m = allocator.allocateFrom(ValueLayout.JAVA_BYTE, bytes);
            // default size
            CompressUtil.compressUsingGzip(m, DeflateBinding.LIBDEFLATE_FASTEST_LEVEL, MemApi.DEFAULT, compressed -> {
                CompressUtil.decompressUsingGzip(compressed, MemApi.DEFAULT, decompressed -> {
                    byte[] decompressedBytes = decompressed.toArray(ValueLayout.JAVA_BYTE);
                    Assertions.assertArrayEquals(bytes, decompressedBytes);
                });
            });

            // fixed size
            CompressUtil.compressUsingGzip(m, DeflateBinding.LIBDEFLATE_FASTEST_LEVEL, MemApi.DEFAULT, compressed -> {
                CompressUtil.decompressUsingGzip(compressed, m.byteSize(), MemApi.DEFAULT, decompressed -> {
                    byte[] decompressedBytes = decompressed.toArray(ValueLayout.JAVA_BYTE);
                    Assertions.assertArrayEquals(bytes, decompressedBytes);
                });
            });
        }
    }

    @Test
    public void testBrotliCompress() {
        try(Allocator allocator = Allocator.newDirectAllocator(MemApi.DEFAULT)) {
            byte[] bytes = str.getBytes(StandardCharsets.UTF_8);
            MemorySegment m = allocator.allocateFrom(ValueLayout.JAVA_BYTE, bytes);
            // default size
            CompressUtil.compressUsingBrotli(m, MemApi.DEFAULT, compressed -> {
                CompressUtil.decompressUsingBrotli(compressed, MemApi.DEFAULT, decompressed -> {
                    byte[] decompressedBytes = decompressed.toArray(ValueLayout.JAVA_BYTE);
                    Assertions.assertArrayEquals(bytes, decompressedBytes);
                });
            });

            // smaller size
            CompressUtil.compressUsingBrotli(m, MemApi.DEFAULT, compressed -> {
                CompressUtil.decompressUsingBrotli(compressed, compressed.byteSize(), MemApi.DEFAULT, decompressed -> {
                    byte[] decompressedBytes = decompressed.toArray(ValueLayout.JAVA_BYTE);
                    Assertions.assertArrayEquals(bytes, decompressedBytes);
                });
            });
        }
    }

    @Test
    public void testZstdCompress() {
        try(Allocator allocator = Allocator.newDirectAllocator(MemApi.DEFAULT)) {
            byte[] bytes = str.getBytes(StandardCharsets.UTF_8);
            MemorySegment m = allocator.allocateFrom(ValueLayout.JAVA_BYTE, bytes);
            CompressUtil.compressUsingZstd(m, MemApi.DEFAULT, compressed -> {
                CompressUtil.decompressUsingZstd(compressed, MemApi.DEFAULT, decompressed -> {
                    byte[] decompressedBytes = decompressed.toArray(ValueLayout.JAVA_BYTE);
                    Assertions.assertArrayEquals(bytes, decompressedBytes);
                });
            });
        }
    }
}
