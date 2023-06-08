package cn.zorcc.common.network;

import cn.zorcc.common.Constants;
import cn.zorcc.common.Mix;
import cn.zorcc.common.ReadBuffer;
import cn.zorcc.common.WriteBuffer;
import cn.zorcc.common.enums.ExceptionType;
import cn.zorcc.common.exception.FrameworkException;
import cn.zorcc.common.pojo.Loc;
import cn.zorcc.common.util.ThreadUtil;
import cn.zorcc.common.wheel.Wheel;

import java.util.concurrent.LinkedTransferQueue;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TransferQueue;
import java.util.concurrent.atomic.AtomicInteger;

public final class Channel {
    private static final Native n = Native.n;
    private final Socket socket;
    private final AtomicInteger state;
    private final Encoder encoder;
    private final Decoder decoder;
    private final Handler handler;
    private final Protocol protocol;
    private final Worker worker;
    /**
     *   Writer virtual thread
     */
    private final Thread writerThread;
    /**
     *   Representing remote server address
     */
    private final Loc loc;
    /**
     *   Current read buffer temp zone, visible only for its worker
     */
    private WriteBuffer tempBuffer;
    /**
     *   Writer task queue
     */
    private final TransferQueue<Object> queue = new LinkedTransferQueue<>();

    public Channel(Socket socket, AtomicInteger state, Encoder e, Decoder d, Handler h, Protocol protocol, Loc loc, Worker worker) {
        this.socket = socket;
        this.state = state;
        this.encoder = e;
        this.decoder = d;
        this.handler = h;
        this.protocol = protocol;
        this.loc = loc;
        this.worker = worker;
        this.writerThread = ThreadUtil.virtual("Ch@" + socket.hashCode(), () -> {
            Thread currentThread = Thread.currentThread();
            try{
                while (!currentThread.isInterrupted()) {
                    Object msg = queue.take();
                    switch (msg) {
                        case Shutdown(long timeout, TimeUnit timeUnit) -> {
                            protocol.doShutdown(this);
                            Wheel.wheel().addJob(() -> worker.submitTask(new Task(Task.TaskType.CLOSE_CHANNEL, null, this, null)), timeout, timeUnit);
                            currentThread.interrupt();
                        }
                        case Mix(Object[] objects) -> {
                            try(WriteBuffer writeBuffer = new WriteBuffer(Net.WRITE_BUFFER_SIZE)) {
                                for (Object o : objects) {
                                    encoder.encode(writeBuffer, o);
                                }
                                protocol.doWrite(this, writeBuffer);
                            }
                        }
                        default -> {
                            try(WriteBuffer writeBuffer = new WriteBuffer(Net.WRITE_BUFFER_SIZE)) {
                                encoder.encode(writeBuffer, msg);
                                protocol.doWrite(this, writeBuffer);
                            }
                        }
                    }
                }
            }catch (InterruptedException i) {
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

    public Handler handler() {
        return handler;
    }

    public Protocol protocol() {
        return protocol;
    }

    public Worker worker() {
        return worker;
    }

    public Loc loc() {
        return loc;
    }

    public Thread writerThread() {
        return writerThread;
    }

    /**
     *   Deal with incoming ReadBuffer, should only be accessed in its worker-thread
     */
    public void onReadBuffer(ReadBuffer buffer) {
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
        Object result = decoder.decode(buffer);
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
     *   Calling shutdown for multiple times doesn't matter since writerThread will exit when handling the first shutdown signal
     */
    public void shutdown(Shutdown shutdown) {
        if (!queue.offer(shutdown)) {
            throw new FrameworkException(ExceptionType.NETWORK, Constants.UNREACHED);
        }
    }

    /**
     *   Shutdown with default timeout configuration
     */
    public void shutdown() {
        shutdown(Shutdown.DEFAULT);
    }

    /**
     *   Close current channel, release it from the worker and clean up
     */
    public void close() {
        if(Thread.currentThread() != worker.thread()) {
            throw new FrameworkException(ExceptionType.NETWORK, "Not in worker thread");
        }
        if(worker.socketMap().remove(socket, this)) {
            // There is no side-effect if the writerThread has been interrupted by shutdown method
            writerThread.interrupt();
            int current = state.getAndSet(Native.REGISTER_NONE);
            if(current > 0) {
                n.ctl(worker.mux(), socket, current, Native.REGISTER_NONE);
            }
            protocol.doClose(this);
            handler.onRemoved(this);
            if (worker.counter().decrementAndGet() == 0L) {
                worker.submitTask(new Task(Task.TaskType.POSSIBLE_SHUTDOWN, null, null, null));
            }
        }
    }
}
