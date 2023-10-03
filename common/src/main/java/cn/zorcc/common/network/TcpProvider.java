package cn.zorcc.common.network;

public final class TcpProvider implements Provider {
    @Override
    public Connector newConnector() {
        return new TcpConnector();
    }

    @Override
    public void close() {

    }
}
