package cn.zorcc.common.rpc;

import cn.zorcc.common.ReadBuffer;
import cn.zorcc.common.network.Decoder;

public final class RpcDecoder implements Decoder {
    @Override
    public Object decode(ReadBuffer readBuffer) {
        long currentIndex = readBuffer.readIndex();
        if(readBuffer.available() < 8) {
            return null;
        }
        int msgType = readBuffer.readInt();
        int len = readBuffer.readInt();
        if(readBuffer.available() < len) {
            readBuffer.setReadIndex(currentIndex);
            return null;
        }
        return new RpcMsg(msgType, len, readBuffer.readHeapSegment(len));
    }
}
