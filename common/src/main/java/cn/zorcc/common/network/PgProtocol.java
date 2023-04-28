package cn.zorcc.common.network;

import cn.zorcc.common.Constants;
import cn.zorcc.common.enums.ExceptionType;
import cn.zorcc.common.exception.FrameworkException;

/**
 *   Postgresql client protocol
 *   First, a TCP connection would be initialized with connect(), then we will bind it to the master thread first and send the SSL request to determine
 *   whether we could use SslProtocol or not, then we bind the channel to target worker.
 */
public final class PgProtocol implements Protocol {
    @Override
    public void canAccept(Channel channel) {
        // postgresql protocol couldn't be used as a server-side protocol
        throw new FrameworkException(ExceptionType.NETWORK, Constants.UNREACHED);
    }

    @Override
    public void canConnect(Channel channel) {
        // TODO after connected to postgresql server, figure out whether it could use SSL connection or not
    }

    @Override
    public void masterCanRead(Channel channel) {

    }

    @Override
    public void masterCanWrite(Channel channel) {

    }

    @Override
    public void workerCanRead(Channel channel) {

    }

    @Override
    public void workerCanWrite(Channel channel) {

    }
}
