package cn.zorcc.common.network;


import cn.zorcc.common.Constants;
import cn.zorcc.common.ReadBuffer;
import cn.zorcc.common.WriteBuffer;
import cn.zorcc.common.enums.ExceptionType;
import cn.zorcc.common.exception.FrameworkException;
import cn.zorcc.common.wheel.Wheel;
import lombok.extern.slf4j.Slf4j;

import java.lang.foreign.MemorySegment;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.locks.LockSupport;

/**
 *   Tcp channel protocol, using system send and recv
 */
@Slf4j
public final class TcpProtocol implements Protocol {
    private static final Native n = Native.n;
    /**
     *   when Protocol is created, the channel is available by default, the availability changed when channel was shutdown or closed
     */
    private final AtomicBoolean availableFlag = new AtomicBoolean(true);
    /**
     *   shutdownFlag is to make sure that doShutdown() method will only be invoked once, by any user thread
     */
    private final AtomicBoolean shutdownFlag = new AtomicBoolean(false);
    /**
     *   closeFlag is to make sure that doClose() method will only be invoked once, by worker or by wheel
     *   there is no need to cancel the timeout wheelJob manually since there are no side effects
     */
    private final AtomicBoolean closeFlag = new AtomicBoolean(false);

    @Override
    public boolean available() {
        return availableFlag.get();
    }

    @Override
    public void canRead(Channel channel, ReadBuffer readBuffer) {
        long readableBytes = n.recv(channel.socket(), readBuffer.segment(), readBuffer.len());
        if(readableBytes > 0) {
            readBuffer.setWriteIndex(readableBytes);
            channel.onReadBuffer(readBuffer);
        }else if(readableBytes == 0) {
            doClose(channel);
        }else {
            throw new FrameworkException(ExceptionType.NETWORK, "Unable to recv, errno : %d".formatted(n.errno()));
        }
    }

    @Override
    public void canWrite(Channel channel) {
        if(channel.state().compareAndSet(Native.REGISTER_READ_WRITE, Native.REGISTER_READ)) {
            n.ctl(channel.worker().state().mux(), channel.socket(), Native.REGISTER_READ_WRITE, Native.REGISTER_READ);
            LockSupport.unpark(channel.writerThread());
        }else {
            throw new FrameworkException(ExceptionType.NETWORK, Constants.UNREACHED);
        }
    }

    @Override
    public void doWrite(Channel channel, WriteBuffer writeBuffer) {
        Socket socket = channel.socket();
        MemorySegment segment = writeBuffer.segment();
        long len = segment.byteSize();
        long bytes = n.send(socket, segment, len);
        if(bytes == -1L) {
            // error occurred
            int errno = n.errno();
            if(errno == n.sendBlockCode()) {
                // current TCP write buffer is full, wait until writable again
                if(channel.state().compareAndSet(Native.REGISTER_READ, Native.REGISTER_READ_WRITE)) {
                    NetworkState workerState = channel.worker().state();
                    n.ctl(workerState.mux(), socket, Native.REGISTER_READ, Native.REGISTER_READ_WRITE);
                    LockSupport.park();
                    doWrite(channel, writeBuffer);
                }else {
                    throw new FrameworkException(ExceptionType.NETWORK, Constants.UNREACHED);
                }
            }else {
                doClose(channel);
            }
        }else if(bytes < len){
            // only write a part of the segment, wait for next loop
            writeBuffer.truncate(bytes);
            doWrite(channel, writeBuffer);
        }
    }

    @Override
    public void doShutdown(Channel channel, long timeout, TimeUnit timeUnit) {
        if(shutdownFlag.compareAndSet(false ,true)) {
            availableFlag.set(false);
            n.shutdownWrite(channel.socket());
            Wheel.wheel().addJob(() -> doClose(channel), timeout, timeUnit);
        }
    }

    @Override
    public void doClose(Channel channel) {
        if(closeFlag.compareAndSet(false, true)) {
            availableFlag.set(false);
            channel.writerThread().interrupt();
            Socket socket = channel.socket();
            NetworkState workerState = channel.worker().state();
            workerState.socketMap().remove(socket, channel);
            int current = channel.state().getAndSet(Native.REGISTER_NONE);
            if(current > 0) {
                n.ctl(workerState.mux(), socket, current, Native.REGISTER_NONE);
            }
            n.closeSocket(socket);
            channel.handler().onClose(channel);
        }
    }
}
