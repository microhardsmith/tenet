package cn.zorcc.common.network;

public record AcceptorState(
        Socket socket,
        Codec codec,
        Handler handler,
        Protocol protocol
) {
}
