package cn.zorcc.orm.pg;

import cn.zorcc.common.ReadBuffer;
import cn.zorcc.common.WriteBuffer;
import cn.zorcc.common.network.Channel;
import cn.zorcc.common.network.Protocol;

import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

public class PgProtocol implements Protocol {
    private final AtomicBoolean availableFlag = new AtomicBoolean(true);

    @Override
    public boolean available() {
        return availableFlag.get();
    }

    @Override
    public void canRead(Channel channel, ReadBuffer readBuffer) {

    }

    @Override
    public void canWrite(Channel channel) {

    }

    @Override
    public void doWrite(Channel channel, WriteBuffer writeBuffer) {

    }

    @Override
    public void doShutdown(Channel channel, long timeout, TimeUnit timeUnit) {

    }

    @Override
    public void doClose(Channel channel) {

    }
}
