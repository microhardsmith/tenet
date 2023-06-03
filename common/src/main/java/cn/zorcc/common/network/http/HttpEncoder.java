package cn.zorcc.common.network.http;

import cn.zorcc.common.Constants;
import cn.zorcc.common.WriteBuffer;
import cn.zorcc.common.enums.ExceptionType;
import cn.zorcc.common.exception.FrameworkException;
import cn.zorcc.common.network.Encoder;

import java.nio.charset.StandardCharsets;
import java.util.Map;

public class HttpEncoder implements Encoder {
    @Override
    public void encode(WriteBuffer writeBuffer, Object o) {
        if(o instanceof HttpRes httpRes) {
            writeBuffer.writeBytes(httpRes.getVersion().getBytes(StandardCharsets.UTF_8));
            writeBuffer.writeByte(Constants.SPACE);
            HttpStatus status = httpRes.getStatus();
            writeBuffer.writeBytes(status.code().getBytes(StandardCharsets.UTF_8));
            writeBuffer.writeByte(Constants.SPACE);
            writeBuffer.writeBytes(status.description().getBytes(StandardCharsets.UTF_8));
            writeBuffer.writeBytes(Constants.CR, Constants.LF);
            Map<String, String> headers = httpRes.getHeaders();
            if(headers != null && !headers.isEmpty()) {
                for (Map.Entry<String, String> entry : headers.entrySet()) {
                    writeBuffer.writeBytes(entry.getKey().getBytes(StandardCharsets.UTF_8));
                    writeBuffer.writeBytes(Constants.COLON, Constants.SPACE);
                    writeBuffer.writeBytes(entry.getValue().getBytes(StandardCharsets.UTF_8));
                    writeBuffer.writeBytes(Constants.CR, Constants.LF);
                }
            }
            writeBuffer.writeBytes(Constants.CR, Constants.LF);
            byte[] data = httpRes.getData();
            if(data != null && data.length != 0) {
                writeBuffer.writeBytes(data);
            }
        }else {
            throw new FrameworkException(ExceptionType.HTTP, "Unrecognized encoding object");
        }
    }
}
