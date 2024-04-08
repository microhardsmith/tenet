package cn.zorcc.common.http;

import cn.zorcc.common.Constants;
import cn.zorcc.common.ExceptionType;
import cn.zorcc.common.exception.FrameworkException;
import cn.zorcc.common.network.Encoder;
import cn.zorcc.common.network.Writer;
import cn.zorcc.common.structure.Allocator;
import cn.zorcc.common.structure.MemApi;
import cn.zorcc.common.structure.WriteBuffer;
import cn.zorcc.common.util.CompressUtil;
import cn.zorcc.common.util.NativeUtil;

import java.lang.foreign.MemorySegment;
import java.nio.charset.StandardCharsets;

public final class HttpServerEncoder implements Encoder {
    @Override
    public void encode(WriteBuffer writeBuffer, Object o) {
        if(o instanceof HttpResponse httpResponse) {
            encodeHttpResponse(writeBuffer, httpResponse);
        }else {
            throw new FrameworkException(ExceptionType.HTTP, "Unrecognized object for encoding");
        }
    }

    private static void encodeHttpResponse(WriteBuffer writeBuffer, HttpResponse httpResponse) {
        writeBuffer.writeBytes(httpResponse.getVersion().getBytes(StandardCharsets.UTF_8));
        writeBuffer.writeByte(Constants.SPACE);
        writeBuffer.writeBytes(httpResponse.getStatus().content());
        writeBuffer.writeBytes(Constants.HTTP_LINE_SEP);
        HttpHeader headers = httpResponse.getHeaders();
        MemorySegment rawData = httpResponse.getData();
        if(rawData == null || rawData.byteSize() == 0L) {
            throw new FrameworkException(ExceptionType.HTTP, "Http response without body is meaningless");
        }
        assert !rawData.isNative();
        switch (httpResponse.getCompressionStatus()) {
            case NONE -> fillData(writeBuffer, headers, rawData);
            case GZIP -> {
                headers.put(HttpHeader.K_CONTENT_ENCODING, HttpHeader.V_GZIP);
                MemApi memApi = Writer.localMemApi();
                try(Allocator allocator = Allocator.newDirectAllocator(memApi)) {
                    CompressUtil.compressUsingGzip(NativeUtil.toNative(rawData, allocator), memApi, data -> fillData(writeBuffer, headers, data));
                }
            }
            case DEFLATE -> {
                headers.put(HttpHeader.K_CONTENT_ENCODING, HttpHeader.V_DEFLATE);
                MemApi memApi = Writer.localMemApi();
                try(Allocator allocator = Allocator.newDirectAllocator(memApi)) {
                    CompressUtil.compressUsingDeflate(NativeUtil.toNative(rawData, allocator), memApi, data -> fillData(writeBuffer, headers, data));
                }
            }
            case BROTLI -> {
                headers.put(HttpHeader.K_CONTENT_ENCODING, HttpHeader.V_BR);
                MemApi memApi = Writer.localMemApi();
                try(Allocator allocator = Allocator.newDirectAllocator(memApi)) {
                    CompressUtil.compressUsingBrotli(NativeUtil.toNative(rawData, allocator), memApi, data -> fillData(writeBuffer, headers, data));
                }
            }
            case ZSTD -> {
                headers.put(HttpHeader.K_CONTENT_ENCODING, HttpHeader.V_ZSTD);
                MemApi memApi = Writer.localMemApi();
                try(Allocator allocator = Allocator.newDirectAllocator(memApi)) {
                    CompressUtil.compressUsingZstd(NativeUtil.toNative(rawData, allocator), memApi, data -> fillData(writeBuffer, headers, data));
                }
            }
        }
    }

    private static void fillData(WriteBuffer writeBuffer, HttpHeader headers, MemorySegment data) {
        headers.put(HttpHeader.K_CONTENT_LENGTH, String.valueOf(data.byteSize()));
        headers.encode(writeBuffer);
        writeBuffer.writeBytes(Constants.HTTP_LINE_SEP);
        writeBuffer.writeSegment(data);
    }
}
