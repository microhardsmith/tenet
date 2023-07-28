package cn.zorcc.common.network;

import cn.zorcc.common.Constants;
import cn.zorcc.common.enums.ExceptionType;
import cn.zorcc.common.exception.FrameworkException;
import cn.zorcc.common.pojo.Loc;

import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.locks.LockSupport;

/**
 *   Network acceptor abstraction
 *   Unlike most application, tenet has divided a connection establish phase into two seperate part: Acceptor and Channel
 *   During the acceptor part, reading and writing will be handled by a unique connector instance
 *   Acceptor can evolve into a Channel, then its reading and writing operations will be take over by Tenet default read-write model, or fail and evicted from the worker
 *   Acceptor and Channel will always use the same worker instance, only one would exist in its socketMap
 *   The acceptor must only be accessed in the worker's reader thread, acceptor will never touch worker's writer thread
 */
public final class Acceptor {
    private static final Native n = Native.n;
    private static final AtomicInteger hashcodeGenerator = new AtomicInteger(Constants.ZERO);
    private final int hashcode = hashcodeGenerator.getAndIncrement();
    private final Socket socket;
    private final Encoder encoder;
    private final Decoder decoder;
    private final Handler handler;
    private final Connector connector;
    private final Worker worker;
    private final Loc loc;
    private final AtomicInteger state = new AtomicInteger(Native.REGISTER_NONE);

    public Acceptor(Socket socket, Encoder encoder, Decoder decoder, Handler handler, Connector connector, Worker worker, Loc loc) {
        this.socket = socket;
        this.encoder = encoder;
        this.decoder = decoder;
        this.handler = handler;
        this.connector = connector;
        this.worker = worker;
        this.loc = loc;
    }

    public int hashcode() {
        return hashcode;
    }

    public Socket socket() {
        return socket;
    }

    public Connector connector() {
        return connector;
    }

    public Worker worker() {
        return worker;
    }

    public AtomicInteger state() {
        return state;
    }

    /**
     *   Evolve current Acceptor to a new created Channel in worker thread
     *   Note that this function should only be invoked by its connector in shouldRead() or shouldWrite() to replace current Acceptor.
     */
    public void toChannel(Protocol protocol) {
        if(Thread.currentThread() != worker.reader()) {
            throw new FrameworkException(ExceptionType.NETWORK, "Not in worker thread");
        }
        Channel channel = new Channel(hashcode, socket, state, encoder, decoder, handler, protocol, loc, worker);
        worker.socketMap().put(socket, channel);
        worker.submitWriterTask(new WriterTask(WriterTask.WriterTaskType.INITIATE, channel, null));
        int from = state.getAndSet(Native.REGISTER_READ);
        if(from != Native.REGISTER_READ) {
            n.ctl(worker.mux(), socket, from, Native.REGISTER_READ);
        }
        handler.onConnected(channel);
    }

    /**
     *   Close current acceptor in worker's reader thread
     *   This method could be directly executed by connector, or scheduled by a wheel job to the taskQueue, only one would succeed
     */
    public void close() {
        if(Thread.currentThread() != worker.reader()) {
            throw new FrameworkException(ExceptionType.NETWORK, "Not in worker thread");
        }
        if(worker.socketMap().remove(socket, this)) {
            int current = state.getAndSet(Native.REGISTER_NONE);
            if(current > 0) {
                n.ctl(worker.mux(), socket, current, Native.REGISTER_NONE);
            }
            connector.doClose(this);
            if (worker.counter().decrementAndGet() == 0L) {
                worker.submitReaderTask(new ReaderTask(ReaderTask.ReaderTaskType.POSSIBLE_SHUTDOWN, null));
            }
        }
    }

}
