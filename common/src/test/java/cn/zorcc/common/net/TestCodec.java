package cn.zorcc.common.net;

import cn.zorcc.common.ReadBuffer;
import cn.zorcc.common.WriteBuffer;
import cn.zorcc.common.network.Codec;

import java.lang.foreign.ValueLayout;
import java.nio.charset.StandardCharsets;
import java.util.Objects;

public class TestCodec implements Codec {
    @Override
    public void encode(WriteBuffer buffer, Object o) {
        if(o instanceof String s) {
            buffer.writeBytes(s.getBytes(StandardCharsets.UTF_8));
        }
    }

    @Override
    public Object decode(ReadBuffer readBuffer) {
        byte[] bytes = Objects.requireNonNull(readBuffer.remaining()).toArray(ValueLayout.JAVA_BYTE);
        return new String(bytes, StandardCharsets.UTF_8);
    }
}
