package cn.zorcc.common.network;

import cn.zorcc.common.Constants;
import cn.zorcc.common.Context;
import cn.zorcc.common.LifeCycle;
import cn.zorcc.common.enums.ExceptionType;
import cn.zorcc.common.exception.FrameworkException;
import cn.zorcc.common.pojo.Loc;
import cn.zorcc.common.util.ConfigUtil;

import java.lang.foreign.Arena;
import java.lang.foreign.MemorySegment;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicLong;
import java.util.function.Function;
import java.util.function.Supplier;

/**
 *   Net is the core of the whole Network application, Net consists of a Master and several Workers
 *   For a network channel, the most essential three components are : codec, handler and protocol
 *   Codec determines how the ReadBuffer should be decoded as a java object, and how to parse a java object into a writeBuffer for transferring
 *   Handler determines how we deal with the incoming data
 *   Protocol determines the low level operations
 */
public class Net implements LifeCycle {
    /**
     *  read buffer maximum size for a read operation, could be changed according to specific circumstances
     */
    public static final int READ_BUFFER_SIZE = 16 * Constants.KB;
    /**
     *  write buffer initial size, will automatically expand, could be changed according to specific circumstances
     */
    public static final int WRITE_BUFFER_SIZE = 4 * Constants.KB;
    /**
     *  socket map initial size, will automatically expand, could be changed according to specific circumstances
     */
    public static final int MAP_SIZE = 1024;

    private static final AtomicBoolean instanceFlag = new AtomicBoolean(false);
    private static final Native n = Native.n;
    private final NetworkConfig config;
    private final Master master;
    private final Worker[] workers;
    private final AtomicLong counter = new AtomicLong(0L);
    private final Supplier<ChannelHandler> handlerSupplier;
    private final Supplier<Codec> codecSupplier;
    private final Function<Socket, Protocol> protocolFunction;

    public Net(Supplier<ChannelHandler> handlerSupplier, Supplier<Codec> codecSupplier, Function<Socket, Protocol> protocolFunction) {
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
        this.protocolFunction = protocolFunction;
        this.master = new Master(this);
        this.workers = new Worker[config.getWorkerCount()];
        for(int sequence = 0; sequence < workers.length; sequence++) {
            workers[sequence] = new Worker(this, sequence);
        }
    }

    /**
     *   create a server-side default codec
     */
    public Codec newCodec() {
        return codecSupplier.get();
    }

    /**
     *   create a server-side default handler
     */
    public ChannelHandler newHandler() {
        return handlerSupplier.get();
    }

    /**
     *   create a server-side default protocol
     */
    public Protocol newProtocol(Socket socket) {
        return protocolFunction.apply(socket);
    }

    /**
     *   validate global network config, throw exception if illegal
     */
    private void validateNetworkConfig() {
        String ip = config.getIp();
        if(!ConfigUtil.checkIp(ip)) {
            throw new FrameworkException(ExceptionType.NETWORK, "IpAddress is not valid : %s".formatted(ip));
        }
        Integer port = config.getPort();
        if(!ConfigUtil.checkPort(port)) {
            throw new FrameworkException(ExceptionType.NETWORK, "Port is not valid : %d".formatted(port));
        }
        Integer workerCount = config.getWorkerCount();
        if(workerCount < 1) {
            throw new FrameworkException(ExceptionType.NETWORK, "Worker count must be at least 1");
        }
    }

    /**
     *   return current network config
     */
    public NetworkConfig config() {
        return config;
    }

    /**
     *   return current net master
     */
    public Master master() {
        return master;
    }

    /**
     *   perform round-robin worker selection
     */
    public Worker nextWorker() {
        int index = (int) (counter.getAndIncrement() % workers.length);
        return workers[index];
    }

    /**
     *   Launch a client connect operation for remote server
     *   After connection is established, a newly created channel will be bound with current Remote instance
     */
    public void connect(Remote remote, Codec codec, ChannelHandler handler, Protocol protocol) {
        Loc loc = remote.loc();
        Socket socket = n.createSocket(config, false);
        Worker worker = nextWorker();
        Channel channel = Channel.forClient(socket, codec, handler, protocol, remote, worker);
        try(Arena arena = Arena.openConfined()) {
            MemorySegment sockAddr = n.createSockAddr(loc, arena);
            if(n.connect(socket, sockAddr)) {
                channel.protocol().canConnect(channel);
            }else {
                int errno = n.errno();
                if(errno == n.connectBlockCode()) {
                    // register channel to the master for write event
                    NetworkState state = master.state();
                    state.registerChannel(channel);
                    n.registerWrite(state.mux(), socket);
                }else {
                    throw new FrameworkException(ExceptionType.NETWORK, "Unable to connect, err : %d".formatted(errno));
                }
            }
        }
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
