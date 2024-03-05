package cn.zorcc.common.rpc;

import cn.zorcc.common.network.Decoder;
import cn.zorcc.common.structure.ReadBuffer;

import java.util.List;

public final class RpcDecoder implements Decoder {
    @Override
    public void decode(ReadBuffer readBuffer, List<Object> entityList) {
        for( ; ; ) {
            long currentIndex = readBuffer.readIndex();
            if(readBuffer.available() < 8) {
                return ;
            }
            int msgType = readBuffer.readInt();
            int len = readBuffer.readInt();
            if(readBuffer.available() < len) {
                readBuffer.setReadIndex(currentIndex);
                return ;
            }
            entityList.add(new RpcMsg(msgType, len, readBuffer.readHeapSegment(len)));
        }
    }
}
