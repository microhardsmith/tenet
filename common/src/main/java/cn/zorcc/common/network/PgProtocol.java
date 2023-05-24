package cn.zorcc.common.network;

import cn.zorcc.common.Constants;
import cn.zorcc.common.ReadBuffer;
import cn.zorcc.common.WriteBuffer;
import cn.zorcc.common.enums.ExceptionType;
import cn.zorcc.common.exception.FrameworkException;

import java.util.concurrent.TimeUnit;

/**
 *   Postgresql client protocol
 *   First, a TCP connection would be initialized with connect(), then we will bind it to the master thread first and send the SSL request to determine
 *   whether we could use SslProtocol or not, then we bind the channel to target worker.
 */
public final class PgProtocol implements Protocol {
    @Override
    public boolean available() {
        return false;
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
