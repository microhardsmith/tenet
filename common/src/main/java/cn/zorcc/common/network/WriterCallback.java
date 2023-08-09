package cn.zorcc.common.network;

/**
 *   Writer CallBack interface, could be used for implementing back-pressure
 *   WriterFuture can't be counted on when implementing business logic, the callback only indicates that data has been transferred from user-space to kernel-space
 */
public interface WriterCallback {
    /**
     *   When data are successfully transferred to the socket buffer, this function would be invoked in writer thread
     *   Don't throw a exception in this method or block current thread
     */
    void onSuccess();

    /**
     *   When socket is exceptionally closed, this function would be invoked in writer thread
     *   Don't throw a exception in this method or block current thread
     */
    void onFailure();
}
