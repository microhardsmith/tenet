package cn.zorcc.common.network;

import cn.zorcc.common.Constants;
import cn.zorcc.common.enums.ExceptionType;
import cn.zorcc.common.exception.FrameworkException;
import cn.zorcc.common.structure.Loc;

import java.lang.foreign.MemorySegment;
import java.time.Duration;
import java.util.concurrent.atomic.AtomicInteger;

/**
 *   Network acceptor abstraction
 *   Unlike most application, tenet has divided a connection establish phase into two separate part: Acceptor and Channel
 *   During the acceptor part, reading and writing will be handled by a unique connector instance
 *   Acceptor can evolve into a Channel, then its reading and writing operations will be take over by Tenet default read-write model, or fail and evicted from the worker
 *   Acceptor and Channel will always use the same worker instance, only one would exist in its socketMap
 *   The acceptor must only be accessed in the worker's reader thread, acceptor will never touch worker's writer thread
 */
public final class Acceptor implements Actor {
    private static final OsNetworkLibrary osNetworkLibrary = OsNetworkLibrary.CURRENT;
    private final Socket socket;
    private final Encoder encoder;
    private final Decoder decoder;
    private final Handler handler;
    private final Connector connector;
    private final Worker worker;
    private final Loc loc;
    private final AtomicInteger state = new AtomicInteger(OsNetworkLibrary.REGISTER_NONE);

    public Acceptor(Socket socket, Encoder encoder, Decoder decoder, Handler handler, Connector connector, Worker worker, Loc loc) {
        this.socket = socket;
        this.encoder = encoder;
        this.decoder = decoder;
        this.handler = handler;
        this.connector = connector;
        this.worker = worker;
        this.loc = loc;
    }

    @Override
    public int hashCode() {
        return socket.intValue();
    }

    public Socket socket() {
        return socket;
    }

    public Worker worker() {
        return worker;
    }

    public AtomicInteger state() {
        return state;
    }

    @Override
    public void canRead(MemorySegment buffer) {
        connector.canRead(this, buffer);
    }

    @Override
    public void canWrite() {
        connector.canWrite(this);
    }

    /**
     *   The actual parameter of duration were ignored since acceptor will always close instantly when shutdown
     */
    @Override
    public void canShutdown(Duration duration) {
        close();
    }

    /**
     *   Evolve current Acceptor to a new created Channel in worker thread
     *   Note that this function should only be invoked by its connector in shouldRead() or shouldWrite() to replace current Acceptor.
     */
    public void toChannel(Protocol protocol) {
        if(Thread.currentThread() != worker.reader()) {
            throw new FrameworkException(ExceptionType.NETWORK, Constants.UNREACHED);
        }
        Channel channel = new Channel(socket, state, encoder, decoder, handler, protocol, loc, worker);
        worker.replace(socket, channel);
        worker.submitWriterTask(new WriterTask(WriterTask.WriterTaskType.INITIATE, channel, null, null));
        int from = state.getAndSet(OsNetworkLibrary.REGISTER_READ);
        if(from != OsNetworkLibrary.REGISTER_READ) {
            osNetworkLibrary.ctl(worker.mux(), socket, from, OsNetworkLibrary.REGISTER_READ);
        }
        handler.onConnected(channel);
    }

    /**
     *   Close current acceptor in worker's reader thread
     *   This method could be directly executed by connector, or scheduled by a wheel job to the taskQueue, only one would succeed
     */
    public void close() {
        if(Thread.currentThread() != worker.reader()) {
            throw new FrameworkException(ExceptionType.NETWORK, Constants.UNREACHED);
        }
        if(worker.unregister(socket, this)) {
            int current = state.getAndSet(OsNetworkLibrary.REGISTER_NONE);
            if(current > OsNetworkLibrary.REGISTER_NONE) {
                osNetworkLibrary.ctl(worker.mux(), socket, current, OsNetworkLibrary.REGISTER_NONE);
            }
            connector.doClose(this);
            if (worker.counter().decrementAndGet() == Constants.ZERO) {
                worker.submitReaderTask(ReaderTask.POSSIBLE_SHUTDOWN_TASK);
            }
        }
    }

}
