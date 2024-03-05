package cn.zorcc.common.network;

import cn.zorcc.common.Constants;
import cn.zorcc.common.ExceptionType;
import cn.zorcc.common.exception.FrameworkException;
import cn.zorcc.common.log.Logger;
import cn.zorcc.common.structure.Allocator;
import cn.zorcc.common.structure.IntMap;
import cn.zorcc.common.structure.TaskQueue;

import java.lang.foreign.MemoryLayout;
import java.lang.foreign.MemorySegment;
import java.lang.foreign.ValueLayout;
import java.time.Duration;
import java.util.concurrent.atomic.AtomicInteger;

public record Poller(
        Mux mux,
        TaskQueue<PollerTask> pollerQueue,
        Thread pollerThread
) {
    private static final Logger log = new Logger(Poller.class);
    private static final AtomicInteger counter = new AtomicInteger(0);
    private static final OsNetworkLibrary osNetworkLibrary = OsNetworkLibrary.CURRENT;

    public static Poller newPoller(NetConfig config) {
        Mux mux = osNetworkLibrary.createMux();
        TaskQueue<PollerTask> pollerQueue = new TaskQueue<>(config.getPollerQueueSize());
        Thread pollerThread = createPollerThread(mux, pollerQueue, config);
        return new Poller(mux, pollerQueue, pollerThread);
    }

    public void submit(PollerTask pollerTask) {
        if (pollerTask == null) {
            throw new FrameworkException(ExceptionType.NETWORK, Constants.UNREACHED);
        }
        pollerQueue.offer(pollerTask);
    }

    private static Thread createPollerThread(Mux mux, TaskQueue<PollerTask> pollerQueue, NetConfig config) {
        int sequence = counter.getAndIncrement();
        return Thread.ofPlatform().name(STR."poller-\{sequence}").unstarted(() -> {
            log.info(STR."Initializing poller thread, sequence : \{sequence}");
            IntMap<PollerNode> nodeMap = IntMap.newTreeMap(config.getPollerMapSize());
            try(Allocator allocator = Allocator.newDirectAllocator()) {
                Timeout timeout = Timeout.of(allocator, config.getPollerMuxTimeout());
                int maxEvents = config.getPollerMaxEvents();
                int readBufferSize = config.getPollerBufferSize();
                // Note that different operating system using different alignment for their event's struct layout, however, malloc just makes it always align with 8 bytes, if not, let's force it
                MemorySegment events = allocator.allocate(MemoryLayout.sequenceLayout(maxEvents, osNetworkLibrary.eventLayout()), Long.SIZE);
                MemorySegment[] reservedArray = new MemorySegment[maxEvents];
                for(int i = 0; i < reservedArray.length; i++) {
                    reservedArray[i] = allocator.allocate(ValueLayout.JAVA_BYTE, readBufferSize);
                }
                int state = Constants.RUNNING;
                for( ; ; ) {
                    int r = osNetworkLibrary.waitMux(mux, events, maxEvents, timeout);
                    state = processTasks(pollerQueue, nodeMap, state);
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

    private static int processTasks(TaskQueue<PollerTask> pollerQueue, IntMap<PollerNode> nodeMap, int currentState) {
        for (PollerTask pollerTask : pollerQueue.elements()) {
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
        return currentState;
    }

    private static void handleBindMsg(IntMap<PollerNode> nodeMap, PollerTask pollerTask) {
        Channel channel = pollerTask.channel();
        if(pollerTask.msg() instanceof Sentry sentry) {
            nodeMap.put(channel.socket().intValue(), new PollerNode.SentryPollerNode(nodeMap, channel, sentry));
        }else {
            throw new FrameworkException(ExceptionType.NETWORK, Constants.UNREACHED);
        }
    }

    private static void handleUnbindMsg(IntMap<PollerNode> nodeMap, PollerTask pollerTask) {
        Channel channel = pollerTask.channel();
        PollerNode pollerNode = nodeMap.get(channel.socket().intValue());
        if(pollerNode instanceof PollerNode.SentryPollerNode sentryPollerNode) {
            sentryPollerNode.onClose(pollerTask);
        }
    }

    private static void handleRegisterMsg(IntMap<PollerNode> nodeMap, PollerTask pollerTask) {
        Channel channel = pollerTask.channel();
        PollerNode pollerNode = nodeMap.get(channel.socket().intValue());
        if(pollerNode != null) {
            pollerNode.onRegisterTaggedMsg(pollerTask);
        }
    }

    private static void handleUnregisterMsg(IntMap<PollerNode> nodeMap, PollerTask pollerTask) {
        Channel channel = pollerTask.channel();
        PollerNode pollerNode = nodeMap.get(channel.socket().intValue());
        if(pollerNode != null) {
            pollerNode.onUnregisterTaggedMsg(pollerTask);
        }
    }

    private static void handleCloseMsg(IntMap<PollerNode> nodeMap, PollerTask pollerTask) {
        Channel channel = pollerTask.channel();
        PollerNode pollerNode = nodeMap.get(channel.socket().intValue());
        if(pollerNode != null) {
            pollerNode.onClose(pollerTask);
        }
    }
}
