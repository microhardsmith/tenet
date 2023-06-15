package cn.zorcc.common.network;

public interface Handler {

    /**
     *   After channel got connected, this function would be invoked
     */
    void onConnected(Channel channel);

    /**
     *   Data has been transferred to current worker
     */
    void onRecv(Channel channel, Object data);

    /**
     *   Before channel got shutdown, this function would be invoked
     *   Some goodbye message could be sent in this function to perform a graceful shutdown
     */
    void onShutdown(Channel channel);

    /**
     *   After connection was shutdown or closed, this function would be invoked
     *   Note that you can't expect sending some data in this function
     */
    void onRemoved(Channel channel);
}
