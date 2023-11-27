package cn.zorcc.common.network;

import cn.zorcc.common.Constants;
import cn.zorcc.common.ExceptionType;
import cn.zorcc.common.exception.FrameworkException;
import cn.zorcc.common.log.Logger;
import cn.zorcc.common.network.api.Sentry;
import cn.zorcc.common.network.lib.OsNetworkLibrary;
import cn.zorcc.common.structure.IntMap;
import cn.zorcc.common.structure.IntPair;
import org.jctools.queues.atomic.MpscUnboundedAtomicArrayQueue;

import java.lang.foreign.Arena;
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
    public Poller(PollerConfig pollerConfig) {
        this.pollerThread = createPollerThread(pollerConfig);
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

    private Thread createPollerThread(PollerConfig pollerConfig) {
        int sequence = counter.getAndIncrement();
        return Thread.ofPlatform().name("poller-" + sequence).unstarted(() -> {
            log.info(STR."Initializing poller thread, sequence : \{sequence}");
            IntMap<PollerNode> nodeMap = new IntMap<>(pollerConfig.getMapSize());
            try(Arena arena = Arena.ofConfined()) {
                Timeout timeout = Timeout.of(arena, pollerConfig.getMuxTimeout());
                int maxEvents = pollerConfig.getMaxEvents();
                int readBufferSize = pollerConfig.getReadBufferSize();
                MemorySegment events = arena.allocate(MemoryLayout.sequenceLayout(maxEvents, osNetworkLibrary.eventLayout()));
                MemorySegment[] reservedArray = new MemorySegment[maxEvents];
                for(int i = 0; i < reservedArray.length; i++) {
                    reservedArray[i] = arena.allocateArray(ValueLayout.JAVA_BYTE, readBufferSize);
                }
                int state = Constants.RUNNING;
                for( ; ; ) {
                    int count = osNetworkLibrary.muxWait(mux, events, maxEvents, timeout);
                    if(count < 0) {
                        int errno = osNetworkLibrary.errno();
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
                    for(int index = 0; index < count; index++) {
                        MemorySegment reserved = reservedArray[index];
                        IntPair pair = osNetworkLibrary.pollerAccess(reserved, events, index);
                        PollerNode pollerNode = nodeMap.get(pair.first());
                        if(pollerNode != null) {
                            int event = pair.second();
                            if(event == Constants.NET_W) {
                                pollerNode.onWritableEvent();
                            }else if(event == Constants.NET_R) {
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
                    if(currentState == Constants.CLOSING) {
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

    private static void handleBindMsg(IntMap<PollerNode> nodeMap, PollerTask pollerTask) {
        Channel channel = pollerTask.channel();
        if(pollerTask.msg() instanceof Sentry sentry) {
            SentryPollerNode sentryPollerNode = new SentryPollerNode(nodeMap, channel, sentry);
            nodeMap.put(channel.socket().intValue(), sentryPollerNode);
            sentryPollerNode.onMounted();
        }else {
            throw new FrameworkException(ExceptionType.NETWORK, Constants.UNREACHED);
        }
    }

    private void handleUnbindMsg(IntMap<PollerNode> nodeMap, PollerTask pollerTask) {
        Channel channel = pollerTask.channel();
        PollerNode pollerNode = nodeMap.get(channel.socket().intValue());
        if(pollerNode instanceof SentryPollerNode sentryPollerNode && pollerTask.msg() instanceof Duration duration) {
            log.info(STR."Connection timeout after \{duration.toMillis()} millis for address : \{channel.loc()}");
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
