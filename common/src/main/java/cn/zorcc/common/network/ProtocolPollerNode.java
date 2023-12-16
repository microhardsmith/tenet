package cn.zorcc.common.network;

import cn.zorcc.common.*;
import cn.zorcc.common.exception.FrameworkException;
import cn.zorcc.common.log.Logger;
import cn.zorcc.common.network.api.Protocol;
import cn.zorcc.common.network.lib.OsNetworkLibrary;
import cn.zorcc.common.structure.IntMap;
import cn.zorcc.common.structure.Mutex;

import java.lang.foreign.Arena;
import java.lang.foreign.MemorySegment;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;

public final class ProtocolPollerNode implements PollerNode {
    private static final OsNetworkLibrary osNetworkLibrary = OsNetworkLibrary.CURRENT;
    private static final Logger log = new Logger(ProtocolPollerNode.class);
    /**
     *   The default map_size for each channel to store their unfinished taggedMsg
     */
    private static final int MAP_SIZE = 16;
    private static final int MAX_LIST_SIZE = 64;
    private final IntMap<PollerNode> nodeMap;
    private final Channel channel;
    private final Protocol protocol;
    private final State channelState;
    private List<Object> entityList = new ArrayList<>(MAX_LIST_SIZE);
    private IntMap<TaggedMsg> msgMap;
    private WriteBuffer tempBuffer;
    private Carrier carrier;

    public ProtocolPollerNode(IntMap<PollerNode> nodeMap, Channel channel, Protocol protocol, State channelState) {
        this.nodeMap = nodeMap;
        this.channel = channel;
        this.protocol = protocol;
        this.channelState = channelState;
    }

    @Override
    public void onReadableEvent(MemorySegment reserved, int len) {
        int r;
        try{
            r = protocol.onReadableEvent(reserved, len);
        }catch (FrameworkException e) {
            log.error("Exception thrown in protocolPollerNode when invoking onReadableEvent()", e);
            close();
            return ;
        }
        if(r >= 0) {
            handleReceived(reserved, len, r);
        }else {
            handleEvent(r);
        }
    }

    @Override
    public void onWritableEvent() {
        int r;
        try{
            r = protocol.onWritableEvent();
        }catch (FrameworkException e) {
            log.error("Exception thrown in protocolPollerNode when invoking onWritableEvent()", e);
            close();
            return ;
        }
        if(r < 0) {
            handleEvent(r);
        }
    }

    /**
     *   Register taggedMsg are trusted with no tag conflicting, developers must make sure of that, otherwise there will be unknown mistakes
     */
    @Override
    public void onRegisterTaggedMsg(PollerTask pollerTask) {
        if(pollerTask.channel() == channel && pollerTask.msg() instanceof TaggedMsg taggedMsg) {
            int tag = taggedMsg.tag();
            if(tag == Channel.SEQ) {
                if(carrier != null) {
                    carrier.cas(Carrier.HOLDER, Carrier.FAILED);
                }
                carrier = taggedMsg.carrier();
            }else {
                if(msgMap == null) {
                    msgMap = new IntMap<>(MAP_SIZE);
                }
                msgMap.put(tag, taggedMsg);
            }
        }
    }

    /**
     *   Unregister taggedMsg only removes the pending msg without modifying it
     */
    @Override
    public void onUnregisterTaggedMsg(PollerTask pollerTask) {
        if(pollerTask.channel() == channel && pollerTask.msg() instanceof TaggedMsg taggedMsg) {
            int tag = taggedMsg.tag();
            if(tag == Channel.SEQ) {
                if(taggedMsg.carrier() == carrier) {
                    carrier.cas(Carrier.HOLDER, Carrier.FAILED);
                    carrier = null;
                }
            }else if(msgMap != null) {
                if(msgMap.remove(tag, taggedMsg)) {
                    taggedMsg.carrier().cas(Carrier.HOLDER, Carrier.FAILED);
                }
            }
        }
    }

    @Override
    public void onClose(PollerTask pollerTask) {
        if(pollerTask.channel() == channel) {
            close();
        }
    }

    @Override
    public void exit(Duration duration) {
        channel.shutdown(duration);
    }

    private void handleEvent(int r) {
        if(r == Constants.NET_W || r == Constants.NET_R || r == Constants.NET_RW) {
            ctl(r);
        }else if(r != Constants.NET_IGNORED) {
            throw new FrameworkException(ExceptionType.NETWORK, Constants.UNREACHED);
        }
    }

    private void ctl(int expected) {
        try(Mutex _ = channelState.withMutex()) {
            int state = channelState.get();
            int current = state & Constants.NET_RW;
            if(current != expected) {
                osNetworkLibrary.ctl(channel.poller().mux(), channel.socket(), current, expected);
                channelState.set((expected - current) + state);
            }
        }
    }

    private void handleReceived(MemorySegment segment, int len, int received) {
        if(received == len) {
            onReceive(segment);
        }else if(received > 0) {
            onReceive(segment.asSlice(0, received));
        }else if(received == 0) {
            close();
        }
    }

    private void onReceive(MemorySegment memorySegment) {
        if(tempBuffer == null) {
            long len = memorySegment.byteSize();
            long readIndex = process(memorySegment);
            if(readIndex != Integer.MIN_VALUE && readIndex < len) {
                tempBuffer = WriteBuffer.newDefaultWriteBuffer(Arena.ofConfined(), len);
                tempBuffer.writeSegment(readIndex == 0 ? memorySegment : memorySegment.asSlice(readIndex, len - readIndex));
            }
        }else {
            tempBuffer.writeSegment(memorySegment);
            long len = tempBuffer.writeIndex();
            long readIndex = process(tempBuffer.toSegment());
            if(readIndex == len) {
                tempBuffer.close();
                tempBuffer = null;
            }else if(readIndex != Integer.MIN_VALUE && readIndex != 0) {
                tempBuffer = tempBuffer.truncate(readIndex);
            }
        }
    }

    private long process(MemorySegment memorySegment) {
        ReadBuffer readBuffer = new ReadBuffer(memorySegment);
        try{
            channel.decoder().decode(readBuffer, entityList);
        }catch (RuntimeException e) {
            log.error("Err occurred in decoder", e);
            close();
            return Integer.MIN_VALUE;
        }
        if(!entityList.isEmpty()) {
            for (Object entity : entityList) {
                TaggedResult taggedResult;
                try{
                    taggedResult = channel.handler().onRecv(channel, entity);
                }catch (RuntimeException e) {
                    log.error("Err occurred in onRecv()", e);
                    close();
                    return Integer.MIN_VALUE;
                }
                if(taggedResult != null) {
                    int tag = taggedResult.tag();
                    if(tag == Channel.SEQ) {
                        if(carrier != null) {
                            carrier.cas(Carrier.HOLDER, taggedResult.entity());
                        }
                    }else {
                        TaggedMsg taggedMsg = msgMap.get(tag);
                        if(taggedMsg != null && msgMap.remove(tag, taggedMsg)) {
                            taggedMsg.carrier().cas(Carrier.HOLDER, taggedResult.entity());
                        }
                    }
                }
            }
            if(entityList.size() > MAX_LIST_SIZE) {
                entityList = new ArrayList<>();
            }else {
                entityList.clear();
            }
        }
        return readBuffer.readIndex();
    }

    private void close() {
        if(nodeMap.remove(channel.socket().intValue(), this)) {
            if (tempBuffer != null) {
                tempBuffer.close();
                tempBuffer = null;
            }
            if (msgMap != null) {
                msgMap.asList().forEach(taggedMsg -> taggedMsg.carrier().cas(Carrier.HOLDER, Carrier.FAILED));
            }
            if (carrier != null) {
                carrier.cas(Carrier.HOLDER, Carrier.FAILED);
            }
            try(Mutex _ = channelState.withMutex()) {
                int state = channelState.get();
                int current = state & Constants.NET_RW;
                if(current != Constants.NET_NONE) {
                    osNetworkLibrary.ctl(channel.poller().mux(), channel.socket(), current, Constants.NET_NONE);
                    state -= (current - Constants.NET_NONE);
                }
                channelState.set(state | Constants.NET_PC);
                if((state & Constants.NET_WC) == Constants.NET_WC) {
                    closeProtocol();
                }else {
                    channel.writer().submit(new WriterTask(WriterTaskType.CLOSE, channel, null, null));
                }
            }
            if(nodeMap.isEmpty()) {
                channel.poller().submit(new PollerTask(PollerTaskType.POTENTIAL_EXIT, null, null));
            }
            try{
                channel.handler().onRemoved(channel);
            }catch (RuntimeException e) {
                log.error("Err occurred in onRemoved()", e);
            }
        }
    }

    private void closeProtocol() {
        try{
            protocol.doClose();
        }catch (RuntimeException e) {
            log.error("Failed to close protocol from poller", e);
        }
    }
}
