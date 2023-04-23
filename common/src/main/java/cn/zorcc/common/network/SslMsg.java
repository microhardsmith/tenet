package cn.zorcc.common.network;

public record SslMsg (
        SslMsgType type,
        Msg msg
){
    public static final SslMsg canRead = new SslMsg(SslMsgType.Read, null);
    public static final SslMsg canWrite = new SslMsg(SslMsgType.Write, null);

    public static SslMsg of(Object entity) {
        return new SslMsg(SslMsgType.User, Msg.of(entity));
    }

    public static SslMsg of(Object entity, Runnable callback) {
        return new SslMsg(SslMsgType.User, Msg.of(entity, callback));
    }
}
