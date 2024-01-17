package cn.zorcc.common.network;

import cn.zorcc.common.Constants;
import cn.zorcc.common.ExceptionType;
import cn.zorcc.common.exception.FrameworkException;
import cn.zorcc.common.log.Logger;
import cn.zorcc.common.network.api.Sentry;
import cn.zorcc.common.network.lib.OsNetworkLibrary;
import cn.zorcc.common.structure.Allocator;
import cn.zorcc.common.structure.IntMap;
import org.jctools.queues.atomic.MpscUnboundedAtomicArrayQueue;

import java.lang.foreign.MemoryLayout;
import java.lang.foreign.MemorySegment;
import java.lang.foreign.ValueLayout;
import java.time.Duration;
import java.util.Queue;
import java.util.concurrent.atomic.AtomicInteger;

public final class Poller {
    private static final Logger log = new Logger(Poller.class);
    private static final AtomicInteger counter = new AtomicInteger(0);
    private static final OsNetworkLibrary osNetworkLibrary = OsNetworkLibrary.CURRENT;
    private final Mux mux = osNetworkLibrary.createMux();
    private final Thread pollerThread;
    private final Queue<PollerTask> readerTaskQueue = new MpscUnboundedAtomicArrayQueue<>(Constants.KB);
    public Poller(NetConfig config) {
        this.pollerThread = createPollerThread(config);
    }

    public Mux mux() {
        return mux;
    }

    public Thread thread() {
        return pollerThread;
    }

    public void submit(PollerTask pollerTask) {
        if (pollerTask == null || !readerTaskQueue.offer(pollerTask)) {
            throw new FrameworkException(ExceptionType.NETWORK, Constants.UNREACHED);
        }
    }

    private Thread createPollerThread(NetConfig config) {
        int sequence = counter.getAndIncrement();
        return Thread.ofPlatform().name(STR."poller-\{sequence}").unstarted(() -> {
            log.info(STR."Initializing poller thread, sequence : \{sequence}");
            IntMap<PollerNode> nodeMap = new IntMap<>(config.getPollerMapSize());
            try(Allocator allocator = Allocator.newDirectAllocator()) {
                Timeout timeout = Timeout.of(allocator, config.getPollerMuxTimeout());
                int maxEvents = config.getPollerMaxEvents();
                int readBufferSize = config.getPollerReadBufferSize();
                MemorySegment events = allocator.allocate(MemoryLayout.sequenceLayout(maxEvents, osNetworkLibrary.eventLayout()));
                MemorySegment[] reservedArray = new MemorySegment[maxEvents];
                for(int i = 0; i < reservedArray.length; i++) {
                    reservedArray[i] = allocator.allocate(ValueLayout.JAVA_BYTE, readBufferSize);
                }
                int state = Constants.RUNNING;
                for( ; ; ) {
                    int r = osNetworkLibrary.muxWait(mux, events, maxEvents, timeout);
                    if(r < 0) {
                        int errno = Math.abs(r);
                        if(errno == osNetworkLibrary.interruptCode()) {
                            return ;
                        }else {
                            throw new FrameworkException(ExceptionType.NETWORK, STR."Mux wait failed with errno : \{errno}");
                        }
                    }
                    state = processTasks(nodeMap, state);
                    if(state == Constants.STOPPED) {
                        break ;
                    }
                    for(int index = 0; index < r; index++) {
                        MemorySegment reserved = reservedArray[index];
                        MuxEvent muxEvent = osNetworkLibrary.access(events, index);
                        PollerNode pollerNode = nodeMap.get(muxEvent.socket());
                        if(pollerNode != null) {
                            long event = muxEvent.event();
                            if(event == Constants.NET_W) {
                                pollerNode.onWritableEvent();
                            }else if(event == Constants.NET_R || event == Constants.NET_OTHER) {
                                pollerNode.onReadableEvent(reserved, readBufferSize);
                            }else {
                                throw new FrameworkException(ExceptionType.NETWORK, Constants.UNREACHED);
                            }
                        }
                    }
                }
            }finally {
                log.info(STR."Exiting poller thread, sequence : \{sequence}");
                osNetworkLibrary.exitMux(mux);
            }
        });
    }

    private int processTasks(IntMap<PollerNode> nodeMap, int currentState) {
        for( ; ; ) {
            PollerTask pollerTask = readerTaskQueue.poll();
            if(pollerTask == null) {
                return currentState;
            }
            switch (pollerTask.type()) {
                case BIND -> handleBindMsg(nodeMap, pollerTask);
                case UNBIND -> handleUnbindMsg(nodeMap, pollerTask);
                case REGISTER -> handleRegisterMsg(nodeMap, pollerTask);
                case UNREGISTER -> handleUnregisterMsg(nodeMap, pollerTask);
                case CLOSE -> handleCloseMsg(nodeMap, pollerTask);
                case POTENTIAL_EXIT -> {
                    if(currentState == Constants.CLOSING && nodeMap.isEmpty()) {
                        return Constants.STOPPED;
                    }
                }
                case EXIT -> {
                    if(currentState == Constants.RUNNING) {
                        if(nodeMap.isEmpty()) {
                            return Constants.STOPPED;
                        }else if(pollerTask.msg() instanceof Duration duration) {
                            nodeMap.asList().forEach(pollerNode -> pollerNode.exit(duration));
                            return Constants.CLOSING;
                        }else {
                            throw new FrameworkException(ExceptionType.NETWORK, Constants.UNREACHED);
                        }
                    }
                }
            }
        }
    }

    private void handleBindMsg(IntMap<PollerNode> nodeMap, PollerTask pollerTask) {
        Channel channel = pollerTask.channel();
        SentryPollerNode sentryPollerNode = switch (pollerTask.msg()) {
            case Sentry sentry -> new SentryPollerNode(nodeMap, channel, sentry, null);
            case SentryWithCallback sentryWithCallback-> new SentryPollerNode(nodeMap, channel, sentryWithCallback.sentry(), sentryWithCallback.runnable());
            default -> throw new FrameworkException(ExceptionType.NETWORK, Constants.UNREACHED);
        };
        nodeMap.put(channel.socket().intValue(), sentryPollerNode);
    }

    private void handleUnbindMsg(IntMap<PollerNode> nodeMap, PollerTask pollerTask) {
        Channel channel = pollerTask.channel();
        PollerNode pollerNode = nodeMap.get(channel.socket().intValue());
        if(pollerNode instanceof SentryPollerNode sentryPollerNode) {
            sentryPollerNode.onClose(pollerTask);
        }
    }

    private void handleRegisterMsg(IntMap<PollerNode> nodeMap, PollerTask pollerTask) {
        Channel channel = pollerTask.channel();
        PollerNode pollerNode = nodeMap.get(channel.socket().intValue());
        if(pollerNode != null) {
            pollerNode.onRegisterTaggedMsg(pollerTask);
        }
    }

    private void handleUnregisterMsg(IntMap<PollerNode> nodeMap, PollerTask pollerTask) {
        Channel channel = pollerTask.channel();
        PollerNode pollerNode = nodeMap.get(channel.socket().intValue());
        if(pollerNode != null) {
            pollerNode.onUnregisterTaggedMsg(pollerTask);
        }
    }

    private void handleCloseMsg(IntMap<PollerNode> nodeMap, PollerTask pollerTask) {
        Channel channel = pollerTask.channel();
        PollerNode pollerNode = nodeMap.get(channel.socket().intValue());
        if(pollerNode != null) {
            pollerNode.onClose(pollerTask);
        }
    }
}
