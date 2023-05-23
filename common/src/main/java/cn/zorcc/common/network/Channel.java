package cn.zorcc.common.network;

import cn.zorcc.common.Constants;
import cn.zorcc.common.ReadBuffer;
import cn.zorcc.common.WriteBuffer;
import cn.zorcc.common.enums.ExceptionType;
import cn.zorcc.common.exception.FrameworkException;
import cn.zorcc.common.pojo.Loc;
import cn.zorcc.common.util.ThreadUtil;

import java.util.concurrent.LinkedTransferQueue;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TransferQueue;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;

public final class Channel {
    private final Socket socket;
    private final AtomicInteger state;
    private final Codec codec;
    private final Handler handler;
    private final Protocol protocol;
    private final Worker worker;
    /**
     *   writer virtual thread
     */
    private final Thread writerThread;
    /**
     *   only exist for client-side channel, for server-side channel would be null
     */
    private final Remote remote;
    /**
     *   representing remote server address
     */
    private final Loc loc;
    /**
     *   current read buffer temp zone, visible only for its worker
     */
    private WriteBuffer tempBuffer;
    /**
     *   writer task queue
     */
    private final TransferQueue<Object> queue = new LinkedTransferQueue<>();
    /**
     *   whether shutdown method has been called, this shutdownFlag is different from protocol's
     *   after shutdown method called, the protocol doShutdown() will be called by writerThread
     */
    private final AtomicBoolean shutdownFlag = new AtomicBoolean(false);

    public Channel(Socket socket, AtomicInteger state, Codec codec, Handler handler, Protocol protocol, Remote remote, Loc loc, Worker worker) {
        this.socket = socket;
        this.state = state;
        this.codec = codec;
        this.handler = handler;
        this.protocol = protocol;
        this.remote = remote;
        this.loc = loc;
        this.worker = worker;
        this.writerThread = ThreadUtil.virtual("Ch@" + socket.hashCode(), () -> {
            Thread currentThread = Thread.currentThread();
            try{
                while (!currentThread.isInterrupted()) {
                    Object msg = queue.take();
                    if(msg instanceof Shutdown(long timeout, TimeUnit timeUnit)) {
                        protocol.doShutdown(this, timeout, timeUnit);
                        currentThread.interrupt();
                    }else {
                        try(WriteBuffer writeBuffer = new WriteBuffer(Net.WRITE_BUFFER_SIZE)) {
                            codec.encode(writeBuffer, msg);
                            protocol.doWrite(this, writeBuffer);
                        }
                    }
                }
            }catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        });
    }

    public Socket socket() {
        return socket;
    }

    public AtomicInteger state() {
        return state;
    }

    public Codec codec() {
        return codec;
    }

    public Handler handler() {
        return handler;
    }

    public Protocol protocol() {
        return protocol;
    }

    public Worker worker() {
        return worker;
    }

    public Remote remote() {
        return remote;
    }

    public Loc loc() {
        return loc;
    }

    public Thread writerThread() {
        return writerThread;
    }

    public boolean available() {
        return protocol().available();
    }

    /**
     *   Deal with incoming ReadBuffer, should only be accessed in its worker-thread
     */
    public void onReadBuffer(ReadBuffer buffer) {
        assert Worker.inWorkerThread();
        if(this.tempBuffer != null) {
            // last time readBuffer read is not complete
            this.tempBuffer.write(buffer.remaining());
            ReadBuffer readBuffer = this.tempBuffer.toReadBuffer();
            tryRead(readBuffer);
            if(readBuffer.remains()) {
                // still incomplete read
                this.tempBuffer.truncate(readBuffer.readIndex());
            }else {
                // writeBuffer can now be released
                this.tempBuffer.close();
                this.tempBuffer = null;
            }
        }else {
            tryRead(buffer);
            if(buffer.remains()) {
                // create a new writeBuffer to maintain the unreadable bytes
                this.tempBuffer = new WriteBuffer(buffer.len());
                this.tempBuffer.write(buffer.remaining());
            }
        }
        // reset read buffer for reuse
        buffer.reset();
    }

    /**
     *   try read from ReadBuffer, should only be accessed in its worker-thread
     *   the actual onRecv method should create a new virtual thread to handle the actual blocking business logic
     */
    private void tryRead(ReadBuffer buffer) {
        Object result = codec.decode(buffer);
        if(result != null) {
            handler.onRecv(this, result);
        }
    }

    /**
     *   send msg over the channel, this method could be invoked from any thread
     *   the msg will be processed by the writer thread, there is no guarantee that the msg will delivery
     *   the caller should provide a timeout mechanism to ensure the msg is not dropped, such as retransmission timeout
     */
    public void send(Object msg) {
        // non-blocking put operation
        if (!queue.offer(msg)) {
            throw new FrameworkException(ExceptionType.NETWORK, Constants.UNREACHED);
        }
    }

    /**
     *   Shutdown current channel's write side, this method could be invoked from any thread
     *   Note that shutdown current channel doesn't block the recv operation, the other side will recv 0 and close the socket
     *   in fact, socket will only be closed when worker thread recv EOF from remote peer
     */
    public void shutdown(Shutdown shutdown) {
        // make sure the shutdown method will be only called once
        if(shutdownFlag.compareAndSet(false, true)) {
            if (!queue.offer(shutdown)) {
                throw new FrameworkException(ExceptionType.NETWORK, Constants.UNREACHED);
            }
        }
    }

    /**
     *   Shutdown with default timeout configuration
     */
    public void shutdown() {
        shutdown(Shutdown.DEFAULT);
    }
}
