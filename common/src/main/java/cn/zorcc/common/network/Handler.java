package cn.zorcc.common.network;

public interface Handler {

    /**
     *   After channel got connected, this function would be invoked
     */
    void onConnected(Channel channel);

    /**
     *   After data has been received, this function would be invoked
     *   Note that the data object was generated be Channel's Decoder
     */
    void onRecv(Channel channel, Object data);

    /**
     *   Before channel got shutdown, this function would be invoked
     *   Note that some goodbye message could be sent in this function to perform a graceful shutdown
     */
    void onShutdown(Channel channel);

    /**
     *   After connection was closed, this function would be invoked
     *   Note that you can't expect sending some data in this function, since the connection has already been closed
     */
    void onRemoved(Channel channel);
}
