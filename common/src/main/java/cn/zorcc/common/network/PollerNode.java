package cn.zorcc.common.network;

import cn.zorcc.common.Carrier;
import cn.zorcc.common.Constants;
import cn.zorcc.common.ExceptionType;
import cn.zorcc.common.exception.FrameworkException;
import cn.zorcc.common.log.Logger;
import cn.zorcc.common.structure.*;

import java.lang.foreign.MemorySegment;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;

public sealed interface PollerNode permits PollerNode.SentryPollerNode, PollerNode.ProtocolPollerNode {
    OsNetworkLibrary osNetworkLibrary = OsNetworkLibrary.CURRENT;
    /**
     *   This function would be invoked when channel become readable
     */
    void onReadableEvent(MemorySegment reserved, long len);

    /**
     *   This function would be invoked when channel become writable
     */
    void onWritableEvent();

    /**
     *   Register a taggedMsg to the pollerNode
     */
    void onRegisterTaggedMsg(PollerTask pollerTask);

    /**
     *   Unregister a taggedMsg from the pollerNode
     */
    void onUnregisterTaggedMsg(PollerTask pollerTask);

    /**
     *   This function would be invoked if channel has exception thrown or needs to be force closed
     */
    void onClose(PollerTask pollerTask);

    /**
     *   Exit current pollerNode
     */
    void exit(Duration duration);

    record SentryPollerNode(
            IntMap<PollerNode> nodeMap,
            Channel channel,
            Sentry sentry,
            MemApi memApi
    ) implements PollerNode {
        private static final Logger log = new Logger(SentryPollerNode.class);

        @Override
        public void onReadableEvent(MemorySegment reserved, long len) {
            try{
                handleEvent(sentry.onReadableEvent(reserved, len));
            }catch (RuntimeException e) {
                log.error("Exception thrown in sentryPollerNode when invoking onReadableEvent()", e);
                close();
            }
        }

        @Override
        public void onWritableEvent() {
            try {
                handleEvent(sentry.onWritableEvent());
            }catch (RuntimeException e) {
                log.error("Exception thrown in sentryPollerNode when invoking onWritableEvent()", e);
                close();
            }
        }

        @Override
        public void onRegisterTaggedMsg(PollerTask pollerTask) {
            throw new FrameworkException(ExceptionType.NETWORK, Constants.UNREACHED);
        }

        @Override
        public void onUnregisterTaggedMsg(PollerTask pollerTask) {
            throw new FrameworkException(ExceptionType.NETWORK, Constants.UNREACHED);
        }

        @Override
        public void onClose(PollerTask pollerTask) {
            if (pollerTask.channel() == channel) {
                close();
            }
        }

        @Override
        public void exit(Duration duration) {
            close();
        }

        private void handleEvent(int r) {
            switch (r) {
                case Constants.NET_UPDATE -> updateToProtocol();
                case Constants.NET_R, Constants.NET_W, Constants.NET_RW -> ctl(r);
                case Constants.NET_IGNORED -> {}
                default -> throw new FrameworkException(ExceptionType.NETWORK, Constants.UNREACHED);
            }
        }

        private void ctl(int expected) {
            IntHolder channelState = channel.state();
            int current = channelState.getValue();
            if(current != expected) {
                osNetworkLibrary.ctlMux(channel.poller().mux(), channel.socket(), current, expected, memApi);
                channelState.setValue(expected);
            }
        }

        private void updateToProtocol() {
            try{
                channel.handler().onConnected(channel);
            }catch (RuntimeException e) {
                log.error("Err occurred in onConnected()", e);
                close();
                return ;
            }
            ctl(Constants.NET_R);
            Protocol protocol = sentry.toProtocol();
            ProtocolPollerNode protocolPollerNode = new ProtocolPollerNode(nodeMap, channel, protocol, memApi);
            nodeMap.replace(channel.socket().intValue(), this, protocolPollerNode);
            channel.writer().submit(new WriterTask(WriterTaskType.INITIATE, channel, protocol, null));
        }

        private void close() {
            if(nodeMap.remove(channel.socket().intValue(), this)) {
                closeSentry();
                if(nodeMap.isEmpty()) {
                    channel.poller().submit(new PollerTask(PollerTaskType.POTENTIAL_EXIT, null, null));
                }
            }
        }

        private void closeSentry() {
            try{
                sentry.doClose();
                channel.handler().onFailed(channel);
            }catch (RuntimeException e) {
                log.error("Failed to close sentry", e);
            }
        }
    }

    final class ProtocolPollerNode implements PollerNode {
        private static final Logger log = new Logger(ProtocolPollerNode.class);
        /**
         *   The default map_size for each channel to store their unfinished taggedMsg
         */
        private static final int MAP_SIZE = 16;
        private static final int MAX_LIST_SIZE = 64;
        private final IntMap<PollerNode> nodeMap;
        private final Channel channel;
        private final Protocol protocol;
        private final MemApi memApi;
        private List<Object> entityList = new ArrayList<>(MAX_LIST_SIZE);
        private WriteBuffer tempBuffer;
        private IntMap<TaggedMsg> msgMap;
        private Carrier carrier;

        public ProtocolPollerNode(IntMap<PollerNode> nodeMap, Channel channel, Protocol protocol, MemApi memApi) {
            this.nodeMap = nodeMap;
            this.channel = channel;
            this.protocol = protocol;
            this.memApi = memApi;
        }

        @Override
        public void onReadableEvent(MemorySegment reserved, long len) {
            long r;
            try{
                r = protocol.onReadableEvent(reserved, len);
            }catch (RuntimeException e) {
                log.error("Exception thrown in protocolPollerNode when invoking onReadableEvent()", e);
                close();
                return ;
            }
            if(r >= 0L) {
                handleReceived(reserved, len, r);
            }else {
                handleEvent(Math.toIntExact(-r));
            }
        }

        @Override
        public void onWritableEvent() {
            long r;
            try{
                r = protocol.onWritableEvent();
            }catch (RuntimeException e) {
                log.error("Exception thrown in protocolPollerNode when invoking onWritableEvent()", e);
                close();
                return ;
            }
            if(r < 0L) {
                handleEvent(Math.toIntExact(-r));
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
                        msgMap = IntMap.newLinkedMap(MAP_SIZE);
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
                    if(carrier != null && taggedMsg.carrier() == carrier) {
                        carrier.cas(Carrier.HOLDER, Carrier.FAILED);
                        carrier = null;
                    }
                }else if(msgMap != null) {
                    if(msgMap.remove(tag, taggedMsg)) {
                        taggedMsg.carrier().cas(Carrier.HOLDER, Carrier.FAILED);
                        if(msgMap.isEmpty()) {
                            msgMap = null;
                        }
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
            switch (r) {
                case Constants.NET_R, Constants.NET_W, Constants.NET_RW -> ctl(r);
                case Constants.NET_IGNORED -> {}
                default -> throw new FrameworkException(ExceptionType.NETWORK, Constants.UNREACHED);
            }
        }

        private void ctl(int expected) {
            channel.state().transform(state -> {
                int current = state & Constants.NET_RW;
                if(current != expected) {
                    osNetworkLibrary.ctlMux(channel.poller().mux(), channel.socket(), current, expected, memApi);
                    return expected - current + state;
                }else {
                    return state;
                }
            }, Thread::onSpinWait);
        }

        private void handleReceived(MemorySegment segment, long len, long received) {
            if(received == len) {
                onReceive(segment);
            }else if(received > 0L) {
                onReceive(segment.asSlice(0L, received));
            }else if(received == 0L) {
                close();
            }
        }

        private void onReceive(MemorySegment memorySegment) {
            if(tempBuffer == null) {
                long len = memorySegment.byteSize();
                long readIndex = process(memorySegment);
                if(readIndex >= 0L && readIndex < len) {
                    tempBuffer = WriteBuffer.newHeapWriteBuffer(len);
                    tempBuffer.writeSegment(readIndex == 0L ? memorySegment : memorySegment.asSlice(readIndex, len - readIndex));
                }
            }else {
                tempBuffer.writeSegment(memorySegment);
                long len = tempBuffer.writeIndex();
                long readIndex = process(tempBuffer.asSegment());
                if(readIndex == len) {
                    tempBuffer.close();
                    tempBuffer = null;
                }else if(readIndex > 0L) {
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
                return -1;
            }
            if(!entityList.isEmpty()) {
                for (Object entity : entityList) {
                    TaggedResult taggedResult;
                    try{
                        taggedResult = channel.handler().onRecv(channel, entity);
                    }catch (RuntimeException e) {
                        log.error("Err occurred in onRecv()", e);
                        close();
                        return -1;
                    }
                    if(taggedResult != null) {
                        int tag = taggedResult.tag();
                        if(tag == Channel.SEQ) {
                            if(carrier != null) {
                                carrier.cas(Carrier.HOLDER, taggedResult.entity());
                                carrier = null;
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
                channel.state().transform(state -> {
                    int current = state & Constants.NET_RW;
                    if(current != Constants.NET_NONE) {
                        osNetworkLibrary.ctlMux(channel.poller().mux(), channel.socket(), current, Constants.NET_NONE, memApi);
                        state -= (current - Constants.NET_NONE);
                    }
                    if((state & Constants.NET_WC) == Constants.NET_WC) {
                        closeProtocol();
                    }else {
                        channel.writer().submit(new WriterTask(WriterTaskType.CLOSE, channel, null, null));
                    }
                    return state | Constants.NET_PC;
                }, Thread::onSpinWait);
                try{
                    channel.handler().onRemoved(channel);
                }catch (RuntimeException e) {
                    log.error("Err occurred in onRemoved()", e);
                }
                if(nodeMap.isEmpty()) {
                    channel.poller().submit(new PollerTask(PollerTaskType.POTENTIAL_EXIT, null, null));
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
}
