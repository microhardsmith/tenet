package cn.zorcc.common.network;


import cn.zorcc.common.Constants;
import cn.zorcc.common.ReadBuffer;
import cn.zorcc.common.WriteBuffer;
import cn.zorcc.common.enums.ExceptionType;
import cn.zorcc.common.exception.FrameworkException;
import cn.zorcc.common.util.ThreadUtil;
import lombok.extern.slf4j.Slf4j;

import java.util.concurrent.LinkedTransferQueue;
import java.util.concurrent.TransferQueue;
import java.util.function.Supplier;

/**
 *   Tcp channel protocol, using system send and recv
 */
@Slf4j
public final class TcpProtocol implements Protocol {
    private static final Native n = Native.n;
    private final TransferQueue<Msg> queue = new LinkedTransferQueue<>();
    private final Thread writerThread;
    public TcpProtocol(Socket socket) {
        this.writerThread = ThreadUtil.virtual("Ch@" + socket.hashCode(), () -> {
            Thread currentThread = Thread.currentThread();
            try{
                while (!currentThread.isInterrupted()) {
                    Msg msg = queue.take();
                    try(WriteBuffer writeBuffer = new WriteBuffer(Net.WRITE_BUFFER_SIZE)) {
                        codec.encode(writeBuffer, msg.entity());
                        doWrite(writeBuffer);
                        msg.tryCallBack();
                    }
                }
            }catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        });
    }

    @Override
    public void canAccept(Channel channel) {

    }

    @Override
    public void canConnect(Channel channel) {

    }

    /**
     *   This should never happen, since master thread should only accept connection for tcp channel
     */
    @Override
    public void masterCanRead(Channel channel) {
        throw new FrameworkException(ExceptionType.NETWORK, Constants.UNREACHED);
    }

    /**
     *   Current channel has successfully established connection, the channel must be a client-side channel
     */
    @Override
    public void masterCanWrite(Channel channel) {
        canConnect(channel);
    }

    @Override
    public void workerCanRead(Channel channel, ReadBuffer readBuffer) {
        int readableBytes = n.recv(channel.socket(), readBuffer.segment(), readBuffer.len());
        if(readableBytes > 0) {
            readBuffer.setWriteIndex(readableBytes);
            channel.onReadBuffer(readBuffer);
        }else if(readableBytes == 0){
            channel.close();
        }else {
            throw new FrameworkException(ExceptionType.NETWORK, Constants.UNREACHED);
        }
    }

    @Override
    public void workerCanWrite(Channel channel) {

    }

    @Override
    public void doWrite(WriteBuffer writeBuffer) {

    }
}
