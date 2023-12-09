package cn.zorcc.common.network;

import cn.zorcc.common.log.Logger;
import cn.zorcc.common.network.api.Channel;

/**
 *   CallBack interface, could be used for implementing back-pressure or other mechanisms
 *   CallBack can't be counted on when implementing business logic, the callback only indicates that data has been transferred from user-space to kernel-space
 *   There is no guarantee that when onSuccess() were called, the data will eventually be transferred to the remote peer
 */
public interface WriterCallback {
    Logger log = new Logger(WriterCallback.class);
    /**
     *   When data are successfully transferred to the socket buffer, this function would be invoked in writer thread
     *   Don't block current writer thread inside this function, if there is a time-consuming logic, try start a new virtual thread to handle it
     */
    void onSuccess(Channel channel);

    /**
     *   When socket is exceptionally closed, this function would be invoked in writer thread
     *   Don't block current writer thread inside this function, if there is a time-consuming logic, try start a new virtual thread to handle it
     */
    void onFailure(Channel channel);

    default void invokeOnSuccess(Channel channel) {
        try{
            onSuccess(channel);
        }catch (RuntimeException e) {
            log.error("Err occurred while invoking onSuccess()", e);
        }
    }

    default void invokeOnFailure(Channel channel) {
        try{
            onFailure(channel);
        }catch (RuntimeException e) {
            log.error("Err occurred while invoking onFailure()", e);
        }
    }
}
