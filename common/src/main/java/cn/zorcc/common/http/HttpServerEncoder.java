package cn.zorcc.common.http;

import cn.zorcc.common.Constants;
import cn.zorcc.common.WriteBuffer;
import cn.zorcc.common.enums.ExceptionType;
import cn.zorcc.common.exception.FrameworkException;
import cn.zorcc.common.network.Encoder;
import cn.zorcc.common.util.CompressUtil;

import java.nio.charset.StandardCharsets;
import java.util.Objects;

public final class HttpServerEncoder implements Encoder {
    @Override
    public WriteBuffer encode(WriteBuffer writeBuffer, Object o) {
        if (Objects.requireNonNull(o) instanceof HttpResponse httpResponse) {
            return encodeHttpResponse(writeBuffer, httpResponse);
        }
        throw new FrameworkException(ExceptionType.HTTP, "Unrecognized encoding object");
    }

    private WriteBuffer encodeHttpResponse(WriteBuffer writeBuffer, HttpResponse httpResponse) {
        writeBuffer.writeBytes(httpResponse.getVersion().getBytes(StandardCharsets.UTF_8));
        writeBuffer.writeByte(Constants.SPACE);
        HttpStatus status = httpResponse.getStatus();
        writeBuffer.writeBytes(status.code().getBytes(StandardCharsets.UTF_8));
        writeBuffer.writeByte(Constants.SPACE);
        writeBuffer.writeBytes(status.description().getBytes(StandardCharsets.UTF_8));
        writeBuffer.writeBytes(Constants.CR, Constants.LF);
        HttpHeader headers = httpResponse.getHeaders();
        byte[] data = httpResponse.getData();
        if(data == null || data.length == Constants.ZERO) {
            throw new FrameworkException(ExceptionType.HTTP, "Http response without a request body is meaningless");
        }
        headers.put(HttpHeader.K_CONTENT_LENGTH, String.valueOf(data.length));
        headers.encode(writeBuffer);
        writeBuffer.writeBytes(Constants.CR, Constants.LF);
        writeBuffer.writeBytes(data);
        return writeBuffer;
    }
}
