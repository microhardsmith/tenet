package cn.zorcc.common.network;

import cn.zorcc.common.Constants;
import cn.zorcc.common.enums.ExceptionType;
import cn.zorcc.common.exception.FrameworkException;
import cn.zorcc.common.log.Logger;

import java.lang.foreign.MemorySegment;

/**
 *   Connector for normal TCP connection
 */
public final class TcpConnector implements Connector {
    private static final Logger log = new Logger(TcpConnector.class);
    private static final OsNetworkLibrary osNetworkLibrary = OsNetworkLibrary.CURRENT;
    private final Receiver receiver;
    private final Acceptor acceptor;

    public TcpConnector(Receiver receiver) {
        this.receiver = receiver;
        this.acceptor = receiver.getAcceptor();
    }

    @Override
    public void canRead(MemorySegment buffer) {
        throw new FrameworkException(ExceptionType.NETWORK , Constants.UNREACHED);
    }

    @Override
    public void canWrite() {
        int errOpt = osNetworkLibrary.getErrOpt(acceptor.socket());
        if(errOpt == 0) {
            receiver.upgradeToChannel();
            receiver.setProtocol(new TcpProtocol(receiver));
        }else {
            log.error(STR."Failed to establish tcp connection, errno : \{errOpt}");
            receiver.close();
        }
    }

    @Override
    public void doClose() {
        osNetworkLibrary.closeSocket(acceptor.socket());
        receiver.setAcceptor(null);
        receiver.setConnector(null);
    }
}
