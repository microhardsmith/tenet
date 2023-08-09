package cn.zorcc.common.example;

import cn.zorcc.common.WriteBuffer;
import cn.zorcc.common.enums.ExceptionType;
import cn.zorcc.common.exception.FrameworkException;
import cn.zorcc.common.network.Encoder;

import java.nio.charset.StandardCharsets;

public final class EchoEncoder implements Encoder {
    @Override
    public WriteBuffer encode(WriteBuffer writeBuffer, Object o) {
        if(o instanceof String str) {
            byte[] bytes = str.getBytes(StandardCharsets.UTF_8);
            writeBuffer.writeInt(bytes.length);
            writeBuffer.writeBytes(bytes);
            return writeBuffer;
        }else {
            throw new FrameworkException(ExceptionType.NETWORK, "Require a string");
        }
    }
}
