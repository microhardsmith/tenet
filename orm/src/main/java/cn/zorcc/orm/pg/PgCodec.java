package cn.zorcc.orm.pg;

import cn.zorcc.common.ReadBuffer;
import cn.zorcc.common.WriteBuffer;
import cn.zorcc.common.network.Codec;

import java.util.function.Consumer;

public class PgCodec implements Codec {
    @Override
    public void encode(WriteBuffer buffer, Object o) {
        if(o instanceof PgCommand command) {
            switch (command) {
                case SSL -> PgUtil.encodeSslMsg(buffer);

            }
        }
    }

    @Override
    public Object decode(ReadBuffer readBuffer) {
        return null;
    }
}
