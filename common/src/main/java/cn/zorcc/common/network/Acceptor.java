package cn.zorcc.common.network;

import cn.zorcc.common.Constants;
import cn.zorcc.common.enums.ExceptionType;
import cn.zorcc.common.exception.FrameworkException;
import cn.zorcc.common.pojo.Loc;

import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;

public final class Acceptor {
    private static final Native n = Native.n;
    private final Socket socket;
    private final Encoder encoder;
    private final Decoder decoder;
    private final Handler handler;
    private final Connector connector;
    private final Worker worker;
    private final Loc loc;
    private final AtomicInteger state = new AtomicInteger(Native.REGISTER_NONE);
    private final AtomicBoolean race = new AtomicBoolean(false);

    public Acceptor(Socket socket, Encoder encoder, Decoder decoder, Handler handler, Connector connector, Worker worker, Loc loc) {
        this.socket = socket;
        this.encoder = encoder;
        this.decoder = decoder;
        this.handler = handler;
        this.connector = connector;
        this.worker = worker;
        this.loc = loc;
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
     *   Upgrade current Acceptor to a new created Channel
     *   Note that this function should only be invoked by its connector in shouldRead() or shouldWrite() to replace current Acceptor.
     */
    public void toChannel(Protocol protocol) {
        if(race.compareAndSet(false, true)) {
            Channel channel = new Channel(socket, state, encoder, decoder, handler, protocol, loc, worker);
            worker.state().socketMap().put(socket, channel);
            int from = state.getAndSet(Native.REGISTER_READ);
            if(from != Native.REGISTER_READ) {
                n.ctl(worker.state().mux(), socket, from, Native.REGISTER_READ);
            }
            handler.onConnected(channel);
            channel.writerThread().start();
        }
    }

    /**
     *   Close current acceptor, release it from the worker and clean up
     */
    public void close() {
        if(race.compareAndSet(false, true)) {
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

}
