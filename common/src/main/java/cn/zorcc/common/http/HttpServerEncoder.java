package cn.zorcc.common.http;

import cn.zorcc.common.Constants;
import cn.zorcc.common.ExceptionType;
import cn.zorcc.common.bindings.DeflateBinding;
import cn.zorcc.common.exception.FrameworkException;
import cn.zorcc.common.network.Encoder;
import cn.zorcc.common.structure.Allocator;
import cn.zorcc.common.structure.WriteBuffer;
import cn.zorcc.common.util.CompressUtil;

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

    private void encodeHttpResponse(WriteBuffer writeBuffer, HttpResponse httpResponse) {
        writeBuffer.writeBytes(httpResponse.getVersion().getBytes(StandardCharsets.UTF_8));
        writeBuffer.writeByte(Constants.SPACE);
        writeBuffer.writeSegment(HttpStatus.getHttpStatusSegment(httpResponse.getStatus()));
        writeBuffer.writeBytes(Constants.HTTP_LINE_SEP);
        HttpHeader headers = httpResponse.getHeaders();
        MemorySegment rawData = httpResponse.getData();
        if(rawData == null || rawData.byteSize() == 0L) {
            throw new FrameworkException(ExceptionType.HTTP, "Http response without body is meaningless");
        }
        MemorySegment data = switch (httpResponse.getCompressionStatus()) {
            case NONE -> rawData;
            case GZIP -> {
                headers.put(HttpHeader.K_CONTENT_ENCODING, HttpHeader.V_GZIP);
                try(Allocator allocator = Allocator.newDirectAllocator()) {
                    yield CompressUtil.compressUsingGzip(rawData, DeflateBinding.LIBDEFLATE_DEFAULT_LEVEL, allocator);
                }
            }
            case DEFLATE -> {
                headers.put(HttpHeader.K_CONTENT_ENCODING, HttpHeader.V_DEFLATE);
                try(Allocator allocator = Allocator.newDirectAllocator()) {
                    yield CompressUtil.compressUsingDeflate(rawData, DeflateBinding.LIBDEFLATE_DEFAULT_LEVEL, allocator);
                }
            }
            case BROTLI -> {
                // TODO add brotli implementation
                throw new FrameworkException(ExceptionType.NETWORK, Constants.UNREACHED);
            }
        };
        headers.put(HttpHeader.K_CONTENT_LENGTH, String.valueOf(data.byteSize()));
        headers.encode(writeBuffer);
        writeBuffer.writeBytes(Constants.HTTP_LINE_SEP);
        writeBuffer.writeSegment(data);
    }
}
