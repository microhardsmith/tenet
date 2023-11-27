package cn.zorcc.common.postgre;

import cn.zorcc.common.ReadBuffer;
import cn.zorcc.common.network.api.Decoder;

import java.lang.foreign.MemorySegment;
import java.util.List;

public final class PgDecoder implements Decoder {
    @Override
    public void decode(ReadBuffer readBuffer, List<Object> entityList) {
        for( ; ; ) {
            long currentIndex = readBuffer.readIndex();
            if (readBuffer.available() < 5) {
                return ;
            }
            byte type = readBuffer.readByte();
            int len = readBuffer.readInt() - 4;
            if(readBuffer.available() < len) {
                readBuffer.setReadIndex(currentIndex);
                return ;
            }
            MemorySegment data = readBuffer.readHeapSegment(len);
            entityList.add(new PgMsg(type, data));
        }
    }
}
