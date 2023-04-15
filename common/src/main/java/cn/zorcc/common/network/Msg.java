package cn.zorcc.common.network;

/**
 *  Wrap msg with callback
 *  Note that callback will be executed in Channel's writer thread, so don't block!
 */
public record Msg (
        Object entity,
        Runnable callback
){
    public static Msg of(Object entity) {
        return new Msg(entity, null);
    }

    public static Msg of(Object entity, Runnable callback) {
        return new Msg(entity, callback);
    }

    /**
     *   Invoke current callback
     */
    public void tryCallBack() {
        if(callback != null) {
            callback.run();
        }
    }
}
