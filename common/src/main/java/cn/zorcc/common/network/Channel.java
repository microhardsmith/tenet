package cn.zorcc.common.network;

import cn.zorcc.common.Constants;
import cn.zorcc.common.LifeCycle;
import cn.zorcc.common.ReadBuffer;
import cn.zorcc.common.WriteBuffer;
import cn.zorcc.common.enums.ExceptionType;
import cn.zorcc.common.exception.FrameworkException;
import cn.zorcc.common.exception.ServiceException;
import cn.zorcc.common.pojo.Loc;
import cn.zorcc.common.util.NativeUtil;
import cn.zorcc.common.util.ThreadUtil;

import java.lang.foreign.MemorySegment;
import java.util.Map;
import java.util.concurrent.LinkedTransferQueue;
import java.util.concurrent.TransferQueue;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.locks.LockSupport;

/**
 *   Channel Interface representing for a connected tcp channel
 */
public final class Channel implements LifeCycle {
    private static final Native n = Native.n;
    private static final long mask = (1 << 9) - 1;
    private final AtomicBoolean available = new AtomicBoolean(false);
    private final ChannelHandler handler;
    private final Socket socket;
    private final Worker worker;
    private final Thread thread;
    /**
     *   only exist for client-side channel
     */
    private final Remote remote;
    /**
     *   representing remote server address
     */
    private final Loc loc;
    private final Codec codec;
    private final String threadNamePrefix;
    private long counter = 0L;
    /**
     *   Visible only for its worker
     */
    private WriteBuffer writeBuffer;
    /**
     *   Writer task queue
     */
    private final TransferQueue<Object> queue = new LinkedTransferQueue<>();
    /**
     *   If current writer is writable, when TCP buffer is full, we could implement backpressure by blocking the write thread
     */
    private volatile boolean writable = true;

    private Channel(Net net, Socket socket, ChannelHandler handler, Codec codec, Remote remote, Loc loc, Worker worker) {
        this.socket = socket;
        this.handler = handler;
        this.codec = codec;
        this.remote = remote;
        this.loc = loc;
        this.worker = worker;
        this.threadNamePrefix = "Ch@" + socket.hashCode() + "-";
        this.thread = ThreadUtil.virtual("socket-" + socket.hashCode(), () -> {
            Thread currentThread = Thread.currentThread();
            int writeBufferInitialSize = net.config().getWriteBufferSize();
            try{
                while (!currentThread.isInterrupted()) {
                    Object msg = queue.take();
                    try(WriteBuffer writeBuffer = new WriteBuffer(writeBufferInitialSize)) {
                        codec.encode(writeBuffer, msg);
                        doWrite(writeBuffer);
                    }
                }
            }catch (InterruptedException e) {
                currentThread.interrupt();
            }
        });
    }

    /**
     *   create a channel that accepted by server, using default handler and codec
     */
    public static Channel forServer(Net net, Socket socket, Loc loc, Worker worker) {
        return new Channel(net, socket, net.newHandler(), net.newCodec(), null, loc, worker);
    }

    /**
     *   create a channel that connected by client, using customized codec
     */
    public static Channel forClient(Net net, Socket socket, Codec codec, Remote remote, Worker worker) {
        return new Channel(net, socket, net.newHandler(), codec, remote, remote.loc(), worker);
    }

    /**
     *   Channel initialization, now channel could read and write
     */
    @Override
    public void init() {
        if(available.compareAndSet(false, true)) {
            // start writer thread
            thread.start();
            // register current channel to worker's map
            NetworkState workerState = worker.state();
            if(NativeUtil.isWindows()) {
                Map<Long, Channel> longMap = workerState.getLongMap();
                longMap.put(socket.longValue(), this);
            }else {
                Map<Integer, Channel> intMap = workerState.getIntMap();
                intMap.put(socket.intValue(), this);
            }
            // register multiplexing events
            n.registerRead(workerState.getMux(), socket);
            // invoke handler's function
            handler.onConnected(this);
            //
            if(remote != null) {
                remote.add(this);
            }
        }
    }

    /**
     *   return current channel's remote info, if it's a server-side channel, remote should be null
     */
    public Remote remote() {
        return remote;
    }

    /**
     *   return remote loc, always non-empty
     */
    public Loc loc() {
        return loc;
    }

    /**
     *   implement write operation
     */
    private void doWrite(WriteBuffer writeBuffer) {
        MemorySegment segment = writeBuffer.segment();
        int len = (int) segment.byteSize();
        int bytes = n.send(socket, segment, len);
        if(bytes == -1) {
            // error occurred
            int errno = n.errno();
            if(errno == n.sendBlockCode()) {
                // current TCP write buffer is full, wait until writable again
                writable = false;
                NetworkState workerState = worker.state();
                n.registerWrite(workerState.getMux(), socket);
                LockSupport.park();
                doWrite(writeBuffer);
            }else {
                throw new FrameworkException(ExceptionType.NETWORK, "Unable to write, errno : %d".formatted(errno));
            }
        }else if(bytes < len){
            // only write a part of the segment, wait for next loop
            writeBuffer.truncate(bytes);
            doWrite(writeBuffer);
        }
    }

    /**
     *   the writer thread can now resume writing
     */
    public void becomeWritable() {
        assert Worker.inWorkerThread();
        writable = true;
        LockSupport.unpark(thread);
    }

    public ChannelHandler handler() {
        return handler;
    }

    /**
     *   deal with ReadBuffer, should only be accessed in its worker-thread
     */
    public void onReadBuffer(ReadBuffer buffer) {
        assert Worker.inWorkerThread();
        if(writeBuffer != null) {
            // last time readBuffer read is not complete
            writeBuffer.write(buffer.remaining());
            ReadBuffer readBuffer = writeBuffer.toReadBuffer();
            tryRead(readBuffer);
            if(readBuffer.remains()) {
                // still incomplete read
                writeBuffer.truncate(readBuffer.readIndex());
            }else {
                // writeBuffer can now be released
                writeBuffer.close();
                writeBuffer = null;
            }
        }else {
            tryRead(buffer);
            if(buffer.remains()) {
                // create a new writeBuffer to maintain the unreadable bytes
                writeBuffer = new WriteBuffer(buffer.len());
                writeBuffer.write(buffer.remaining());
            }
        }
    }

    /**
     *   try read from ReadBuffer, should only be accessed in its worker-thread
     */
    private void tryRead(ReadBuffer buffer) {
        Object result = codec.decode(buffer);
        if(result != null) {
            // creating a new virtual thread for handing msg
            ThreadUtil.virtual(threadNamePrefix + (counter++ & mask), () -> handler.onRecv(this, result)).start();
        }
    }

    /**
     *   send msg over the channel, this method could be invoked from any thread
     *   the msg will be processed by the writer thread, there is no guarantee that the msg will delivery
     *   the caller should provide a timeout mechanism to ensure the msg is not dropped
     */
    public void send(Object msg) {
        // send operation must be in a virtual thread because of the potential block
        if(writable) {
            // non-blocking put operation
            if (!queue.offer(msg)) {
                throw new FrameworkException(ExceptionType.NETWORK, Constants.UNREACHED);
            }
        }else {
            // discard current msg by throwing a exception
            throw new ServiceException("Channel is not writable");
        }
    }

    /**
     *   return if the channel is available (not shutdown)
     */
    public boolean isAvailable() {
        return available.get();
    }

    /**
     *   closing current channel, this method could be invoked from any thread
     */
    @Override
    public void shutdown() {
        if(available.compareAndSet(true, false)) {
            NetworkState workerState = worker.state();
            Channel channel = NativeUtil.isWindows() ? workerState.getLongMap().remove(socket.longValue()) : workerState.getIntMap().remove(socket.intValue());
            // current Channel might be closed by other threads
            if(channel != null) {
                n.unregister(workerState.getMux(), socket);
                thread.interrupt();
                n.closeSocket(socket);
                handler.onClose(this);
            }
        }
    }
}
