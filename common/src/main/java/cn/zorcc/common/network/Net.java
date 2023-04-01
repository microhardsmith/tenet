package cn.zorcc.common.network;

import cn.zorcc.common.Constants;
import cn.zorcc.common.Context;
import cn.zorcc.common.LifeCycle;
import cn.zorcc.common.enums.ExceptionType;
import cn.zorcc.common.exception.FrameworkException;
import cn.zorcc.common.util.ConfigUtil;

import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicLong;
import java.util.function.Supplier;

/**
 *   Net is the core of the whole Network application, Net consists of a Master and several Workers
 */
public class Net implements LifeCycle {
    private static final AtomicBoolean instanceFlag = new AtomicBoolean(false);
    private static final Native n = Native.n;
    private final NetworkConfig config;
    private final Master master;
    private final Worker[] workers;
    private final AtomicLong counter = new AtomicLong(0L);
    private final Supplier<ChannelHandler> handlerSupplier;
    private final Supplier<Codec> codecSupplier;

    public Net(Supplier<ChannelHandler> handlerSupplier, Supplier<Codec> codecSupplier) {
        if(!instanceFlag.compareAndSet(false, true)) {
            throw new FrameworkException(ExceptionType.NETWORK, Constants.SINGLETON_MSG);
        }
        this.config = Context.getContainer(NetworkConfig.class);
        if(config == null) {
            throw new FrameworkException(ExceptionType.NETWORK, "Configuration not found");
        }
        validateNetworkConfig();
        this.handlerSupplier = handlerSupplier;
        this.codecSupplier = codecSupplier;
        this.master = new Master(this);
        this.workers = new Worker[config.getWorkerCount()];
        for(int sequence = 0; sequence < workers.length; sequence++) {
            workers[sequence] = new Worker(this, sequence);
        }
    }

    public ChannelHandler newHandler() {
        return handlerSupplier.get();
    }

    public Codec newCodec() {
        return codecSupplier.get();
    }

    /**
     *   validate global network config
     */
    private void validateNetworkConfig() {
        if(!ConfigUtil.checkIp(config.getIp())) {
            throw new FrameworkException(ExceptionType.NETWORK, "IpAddress is not valid");
        }
    }

    public NetworkConfig config() {
        return config;
    }

    public Master master() {
        return master;
    }

    /**
     *   perform round robin worker selection
     */
    public Worker nextWorker() {
        int index = (int) (counter.getAndIncrement() % workers.length);
        return workers[index];
    }

    /**
     *   launch a client connect for remote server, after connection is established, a newly created channel will be bound with current Remote instance
     */
    public void connect(Remote remote, Codec codec) {
        n.connect(this, remote, codec);
    }

    /**
     *   launch a client connect for remote server, using default codec
     */
    public void connect(Remote remote) {
        connect(remote, codecSupplier.get());
    }

    @Override
    public void init() {
        master.init();
        for (Worker worker : workers) {
            worker.init();
        }
    }

    @Override
    public void shutdown() {
        master.shutdown();
        for (Worker worker : workers) {
            worker.shutdown();
        }
        n.exit();
    }
}
