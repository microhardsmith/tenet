package cn.zorcc.common.postgre;

import cn.zorcc.common.Constants;
import cn.zorcc.common.ExceptionType;
import cn.zorcc.common.exception.FrameworkException;
import cn.zorcc.common.network.Encoder;
import cn.zorcc.common.structure.WriteBuffer;

import java.lang.foreign.MemorySegment;

public final class PgEncoder implements Encoder {
    @Override
    public void encode(WriteBuffer writeBuffer, Object o) {
        if(o instanceof PgMsg pgMsg) {
            byte type = pgMsg.type();
            if(type != Constants.NUT) {
                writeBuffer.writeByte(type);
            }
            MemorySegment data = pgMsg.data();
            int len = (int) data.byteSize();
            writeBuffer.writeInt(len + 4);
            if(len > 0) {
                writeBuffer.writeSegment(data);
            }
        }else {
            throw new FrameworkException(ExceptionType.POSTGRESQL, Constants.UNREACHED);
        }
    }
}
