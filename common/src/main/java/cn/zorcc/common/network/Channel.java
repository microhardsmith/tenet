package cn.zorcc.common.network;

import cn.zorcc.common.Constants;
import cn.zorcc.common.LifeCycle;
import cn.zorcc.common.ReadBuffer;
import cn.zorcc.common.WriteBuffer;
import cn.zorcc.common.enums.ExceptionType;
import cn.zorcc.common.exception.FrameworkException;
import cn.zorcc.common.pojo.Loc;
import cn.zorcc.common.util.NativeUtil;
import cn.zorcc.common.util.ThreadUtil;

import java.lang.foreign.MemorySegment;
import java.util.concurrent.LinkedTransferQueue;
import java.util.concurrent.TransferQueue;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.locks.LockSupport;

/**
 *   Channel Interface representing for a connected tcp channel
 */
public final class Channel implements LifeCycle {
    private static final Native n = Native.n;
    private final Codec codec;
    private final ChannelHandler handler;
    private final Protocol protocol;
    private final Socket socket;
    private final Worker worker;
    /**
     *   Read mask for creating virtual thread names
     *   For each read operation, a new virtual thread would be created to handle the process
     *   And the sequence number would be refreshed since it reach mask
     */
    private static final long mask = (1 << 9) - 1;
    /**
     *   whether the channel has been shutdown
     */
    private final AtomicBoolean available = new AtomicBoolean(false);
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
     *   read virtual thread name prefix
     */
    private final String readThreadPrefix;
    /**
     *   virtual thread name counter, only the assigned Worker thread would visit this, so there is no need for a AtomicLong
     */
    private long counter = 0L;
    /**
     *   current read buffer zone, visible only for its worker
     */
    private WriteBuffer buffer;
    /**
     *   Writer task queue
     */
    private final TransferQueue<Msg> queue = new LinkedTransferQueue<>();

    private Channel(Socket socket, Codec codec, ChannelHandler handler, Protocol protocol, Remote remote, Loc loc, Worker worker) {
        this.socket = socket;
        this.codec = codec;
        this.handler = handler;
        this.protocol = protocol;
        this.remote = remote;
        this.loc = loc;
        this.worker = worker;
        this.readThreadPrefix = "Ch@" + socket.hashCode() + "-";
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

    /**
     *   create a channel that accepted by server, using server-side default handler,codec and protocol
     */
    public static Channel forServer(Net net, ClientSocket clientSocket) {
        Socket socket = clientSocket.socket();
        Loc loc = clientSocket.loc();
        return new Channel(socket, net.newCodec(), net.newHandler(), net.newProtocol(socket), null, loc, net.nextWorker());
    }

    /**
     *   create a channel that connected by client, using customized codec
     */
    public static Channel forClient(Socket socket, Codec codec, ChannelHandler handler, Protocol protocol, Remote remote, Worker worker) {
        return new Channel(socket, codec, handler, protocol, remote, remote.loc(), worker);
    }

    /**
     *   return current channel's socket
     */
    public Socket socket() {
        return socket;
    }

    /**
     *   return current channel's writer thread
     */
    public Thread writerThread() {
        return writerThread;
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
     *   return current channel's handler
     */
    public ChannelHandler handler() {
        return handler;
    }

    public Protocol protocol() {
        return protocol;
    }

    /**
     *   implement write operation with recursion
     *   when this method returns, the writeBuffer's data are transferred to the OS
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
                NetworkState workerState = worker.state();
                n.registerWrite(workerState.mux(), socket);
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
     *   deal with ReadBuffer, should only be accessed in its worker-thread
     */
    public void onReadBuffer(ReadBuffer buffer) {
        assert Worker.inWorkerThread();
        if(this.buffer != null) {
            // last time readBuffer read is not complete
            this.buffer.write(buffer.remaining());
            ReadBuffer readBuffer = this.buffer.toReadBuffer();
            tryRead(readBuffer);
            if(readBuffer.remains()) {
                // still incomplete read
                this.buffer.truncate(readBuffer.readIndex());
            }else {
                // writeBuffer can now be released
                this.buffer.close();
                this.buffer = null;
            }
        }else {
            tryRead(buffer);
            if(buffer.remains()) {
                // create a new writeBuffer to maintain the unreadable bytes
                this.buffer = new WriteBuffer(buffer.len());
                this.buffer.write(buffer.remaining());
            }
        }
        // reset read buffer for reuse
        buffer.reset();
    }

    /**
     *   try read from ReadBuffer, should only be accessed in its worker-thread
     */
    private void tryRead(ReadBuffer buffer) {
        Object result = codec.decode(buffer);
        if(result != null) {
            // creating a new virtual thread for handing msg
            ThreadUtil.virtual(readThreadPrefix + (counter++ & mask), () -> handler.onRecv(this, result)).start();
        }
    }

    /**
     *   send msg over the channel, this method could be invoked from any thread
     *   the msg will be processed by the writer thread, there is no guarantee that the msg will delivery
     *   the caller should provide a timeout mechanism to ensure the msg is not dropped
     */
    public void send(Msg msg) {
        // check if current channel has been shutdown
        if(!available.get()) {
            throw new FrameworkException(ExceptionType.NETWORK, "Unable to write to a channel which has been shutdown");
        }
        // non-blocking put operation
        if (!queue.offer(msg)) {
            throw new FrameworkException(ExceptionType.NETWORK, Constants.UNREACHED);
        }
    }

    /**
     *   Return current channel's available flag (not shutdown)
     */
    public AtomicBoolean available() {
        return available;
    }

    /**
     *   Channel initialization, now channel could read and write
     *   should only be accessed in Master's thread
     */
    @Override
    public void init() {
        assert Master.inMasterThread();
        if(available.compareAndSet(false, true)) {
            // start writer thread
            writerThread.start();
            // register current channel to worker's map
            NetworkState workerState = worker.state();
            workerState.registerChannel(this);
            // register multiplexing events
            n.registerRead(workerState.mux(), socket);
            // invoke handler's function
            handler.onConnected(this);
            // add current channel to remote
            if(remote != null) {
                remote.add(this);
            }
        }
    }

    /**
     *   Shutdown current channel, this method could be invoked from any thread
     *   Note that shutdown current channel doesn't block the recv operation, the other side will recv 0 and close the socket
     *   in fact, socket will only be closed when worker thread recv EOF from remote peer
     */
    @Override
    public void shutdown() {
        // if current channel has already been closed, there is no need to shut down
        if(available.compareAndSet(true, false)) {
            writerThread.interrupt();
            n.shutdownWrite(socket);
        }
    }

    /**
     *   Close current channel, this method could only be invoked from worker thread
     */
    public void close() {
        assert Worker.inWorkerThread();
        if (available.getAndSet(false)) {
            // current channel hasn't called shutdown method, we need to interrupt the writer thread
            writerThread.interrupt();
        }
        NetworkState workerState = worker.state();
        if(NativeUtil.isWindows()) {
            workerState.longMap().remove(socket.longValue());
        }else {
            workerState.intMap().remove(socket.intValue());
        }
        n.unregister(workerState.mux(), socket);
        n.closeSocket(socket);
        handler.onClose(this);
    }
}
