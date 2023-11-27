package cn.zorcc.common.http;

import cn.zorcc.common.Constants;
import cn.zorcc.common.ExceptionType;
import cn.zorcc.common.WriteBuffer;
import cn.zorcc.common.exception.FrameworkException;
import cn.zorcc.common.network.api.Encoder;

import java.lang.foreign.MemorySegment;
import java.nio.charset.StandardCharsets;

public final class HttpServerEncoder implements Encoder {
    @Override
    public void encode(WriteBuffer writeBuffer, Object o) {
        if(o instanceof HttpResponse httpResponse) {
            encodeHttpResponse(writeBuffer, httpResponse);
        }else {
            throw new FrameworkException(ExceptionType.HTTP, "Unrecognized encoding object");
        }
    }

    private void encodeHttpResponse(WriteBuffer writeBuffer, HttpResponse httpResponse) {
        writeBuffer.writeBytes(httpResponse.getVersion().getBytes(StandardCharsets.UTF_8));
        writeBuffer.writeByte(Constants.SPACE);
        HttpStatus status = httpResponse.getStatus();
        writeBuffer.writeBytes(status.code().getBytes(StandardCharsets.UTF_8));
        writeBuffer.writeByte(Constants.SPACE);
        writeBuffer.writeBytes(status.description().getBytes(StandardCharsets.UTF_8));
        writeBuffer.writeBytes(Constants.CR, Constants.LF);
        HttpHeader headers = httpResponse.getHeaders();
        MemorySegment data = httpResponse.getData();
        if(data == null || data.byteSize() == 0) {
            throw new FrameworkException(ExceptionType.HTTP, "Http response without a request body is meaningless");
        }
        headers.put(HttpHeader.K_CONTENT_LENGTH, String.valueOf(data.byteSize()));
        headers.encode(writeBuffer);
        writeBuffer.writeBytes(Constants.CR, Constants.LF);
        writeBuffer.writeSegment(data);
    }
}
