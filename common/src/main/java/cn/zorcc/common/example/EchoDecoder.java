package cn.zorcc.common.example;

import cn.zorcc.common.ReadBuffer;
import cn.zorcc.common.network.Decoder;

import java.nio.charset.StandardCharsets;

public final class EchoDecoder implements Decoder {
    @Override
    public Object decode(ReadBuffer readBuffer) {
        long size = readBuffer.size();
        if(size < 4) {
            return null;
        }
        int msgLength = readBuffer.readInt();
        if(size < msgLength + 4) {
            return null;
        }
        return new String(readBuffer.readBytes(msgLength), StandardCharsets.UTF_8);
    }
}
