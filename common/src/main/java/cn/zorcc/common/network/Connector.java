package cn.zorcc.common.network;

/**
 *   Network connector interface, define the behavior of the acceptor
 */
public interface Connector {

    void shouldCancel(Socket socket);

    void shouldClose(Socket socket);

    void shouldRead(Acceptor acceptor);

    void shouldWrite(Acceptor acceptor);
}
