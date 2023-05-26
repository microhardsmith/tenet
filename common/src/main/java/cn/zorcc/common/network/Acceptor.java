package cn.zorcc.common.network;

import cn.zorcc.common.Constants;
import cn.zorcc.common.enums.ExceptionType;
import cn.zorcc.common.exception.FrameworkException;
import cn.zorcc.common.pojo.Loc;
import cn.zorcc.common.wheel.Job;

import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Supplier;

public class Acceptor {
    private static final Native n = Native.n;
    private final Socket socket;
    private final Supplier<Codec> codecSupplier;
    private final Supplier<Handler> handlerSupplier;
    private final Connector connector;
    private final Worker worker;
    private final Job cancelJob;
    private final Remote remote;
    private final Loc loc;
    private final AtomicInteger state = new AtomicInteger(Native.REGISTER_NONE);

    public Acceptor(Socket socket, Supplier<Codec> codecSupplier, Supplier<Handler> handlerSupplier, Connector connector, Worker worker, Remote remote, Loc loc, Job cancelJob) {
        this.socket = socket;
        this.codecSupplier = codecSupplier;
        this.handlerSupplier = handlerSupplier;
        this.connector = connector;
        this.worker = worker;
        this.remote = remote;
        this.loc = loc;
        this.cancelJob = cancelJob;
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

    public void cancelTimeout() {
        if(cancelJob != null) {
            cancelJob.cancel();
        }
    }

    /**
     *   Upgrade current Acceptor to a new created Channel
     *   Note that this function should only be invoked by its connector in shouldRead() or shouldWrite() to replace current Acceptor.
     */
    public void toChannel(Protocol protocol) {
        Codec codec = codecSupplier.get();
        Handler handler = handlerSupplier.get();
        Channel channel = new Channel(socket, state, codec, handler, protocol, remote, loc, worker);
        worker.state().socketMap().put(socket, channel);
        int from = state.getAndSet(Native.REGISTER_READ);
        if(from != Native.REGISTER_READ) {
            n.ctl(worker.state().mux(), socket, from, Native.REGISTER_READ);
        }
        handler.onConnected(channel);
        channel.writerThread().start();
    }

    /**
     *   unbind current Acceptor with worker
     */
    public void unbind() {
        if (worker.state().socketMap().remove(socket, this)) {
            int current = state.getAndSet(Native.REGISTER_NONE);
            if(current > 0) {
                n.ctl(worker.state().mux(), socket, current, Native.REGISTER_NONE);
            }
            connector.shouldClose(socket);
        }else {
            throw new FrameworkException(ExceptionType.NETWORK, Constants.UNREACHED);
        }
    }

}
