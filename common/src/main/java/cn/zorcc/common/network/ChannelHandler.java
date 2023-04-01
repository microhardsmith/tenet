package cn.zorcc.common.network;

import cn.zorcc.common.pojo.Loc;

public interface ChannelHandler {

    /**
     *   After channel got connected, this function would be invoked
     *   you can send some data in this function since connection is fully established
     */
    void onConnected(Loc loc);

    /**
     *   Data has been transferred to current worker
     *   This function runs in a newly created virtual thread
     */
    void onRecv(Channel channel, Object data);

    /**
     *   After connection was closed, this function would be invoked
     *   Note that you can't expect sending some data in this function, since channel has already been closed
     */
    void onClose();
}
