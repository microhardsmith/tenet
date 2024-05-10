package cn.zorcc.common.network;

import cn.zorcc.common.Constants;
import cn.zorcc.common.ExceptionType;
import cn.zorcc.common.Ref;
import cn.zorcc.common.exception.FrameworkException;
import cn.zorcc.common.log.Logger;
import cn.zorcc.common.structure.*;
import cn.zorcc.common.util.NativeUtil;

import java.lang.foreign.MemorySegment;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.function.Consumer;

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

    final class SentryPollerNode implements PollerNode {
        private static final Logger log = new Logger(SentryPollerNode.class);
        private final IntMap<PollerNode> nodeMap;
        private final Channel channel;
        private final Sentry sentry;
        private final MemApi memApi;

        /**
         *   When channel was mounted on the Poller thread, the initial state must be NET_W
         */
        private int state = Constants.NET_W;

        public SentryPollerNode(IntMap<PollerNode> nodeMap, Channel channel, Sentry sentry, MemApi memApi) {
            this.nodeMap = nodeMap;
            this.channel = channel;
            this.sentry = sentry;
            this.memApi = memApi;
        }

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

        /**
         *   Ctl in sentryPollerNode is safe, no external synchronization needed
         */
        private void ctl(int expected) {
            if(state != expected) {
                osNetworkLibrary.ctlMux(channel.poller().mux(), channel.socket(), state, expected, memApi);
                state = expected;
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
            Mutex mutex = new Mutex(channel.poller().pollerThread(), channel.writer().writerThread(), state);
            ProtocolPollerNode protocolPollerNode = new ProtocolPollerNode(nodeMap, channel, protocol, mutex, memApi);
            nodeMap.replace(channel.socket().intValue(), this, protocolPollerNode);
            channel.writer().submit(new WriterTask(WriterTaskType.INITIATE, channel, new ProtocolWithMutex(protocol, mutex), null));
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
        private final Mutex mutex;
        private final MemApi memApi;
        private List<Object> entityList = new ArrayList<>(MAX_LIST_SIZE);
        private WriteBuffer tempBuffer;
        private RefMap refMap;
        private Ref seqRef;

        public ProtocolPollerNode(IntMap<PollerNode> nodeMap, Channel channel, Protocol protocol, Mutex mutex, MemApi memApi) {
            this.nodeMap = nodeMap;
            this.channel = channel;
            this.protocol = protocol;
            this.mutex = mutex;
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
            if(pollerTask.channel() == channel && pollerTask.msg() instanceof TagWithRef tr) {
                MemorySegment tag = tr.tag();
                if(tag == MemorySegment.NULL) {
                    if(seqRef != null) {
                        seqRef.assign(Channel.FAILED);
                    }
                    seqRef = tr.ref();
                }else {
                    if(refMap == null) {
                        refMap = RefMap.newInstance(MAP_SIZE);
                    }
                    refMap.put(tag, tr.ref());
                }
            }
        }

        /**
         *   Unregister taggedMsg only removes the pending msg without modifying it
         */
        @Override
        public void onUnregisterTaggedMsg(PollerTask pollerTask) {
            if(pollerTask.channel() == channel && pollerTask.msg() instanceof TagWithRef taggedMsg) {
                MemorySegment tag = taggedMsg.tag();
                if(tag == MemorySegment.NULL) {
                    if(seqRef != null && taggedMsg.ref() == seqRef) {
                        seqRef.assign(Channel.FAILED);
                        seqRef = null;
                    }
                }else if(refMap != null) {
                    Ref ref = taggedMsg.ref();
                    if(refMap.remove(tag, ref)) {
                        ref.assign(Channel.FAILED);
                        if(refMap.isEmpty()) {
                            refMap = null;
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
            int state = mutex.pLock();
            try {
                int current = state & Constants.NET_RW;
                if(current != expected) {
                    osNetworkLibrary.ctlMux(channel.poller().mux(), channel.socket(), current, expected, memApi);
                    state += expected - current;
                }
            } finally {
                mutex.pUnlock(state);
            }
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
                    // Caching policy could use heap or non-heap, since we could manage it ourselves, let's save the trouble for GC
                    tempBuffer = WriteBuffer.newNativeWriteBuffer(Poller.localMemApi(), len);
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
                    Optional<TagMsg> tm;
                    try{
                        tm = channel.handler().onRecv(channel, entity);
                    }catch (RuntimeException e) {
                        log.error("Err occurred in onRecv()", e);
                        close();
                        return -1;
                    }
                    tm.ifPresent(tagMsg -> {
                        MemorySegment tag = tagMsg.tag();
                        if(tag == MemorySegment.NULL) {
                            if(seqRef != null) {
                                seqRef.assign(tagMsg.msg());
                                seqRef = null;
                            }
                        } else {
                            Ref ref = refMap.get(tag);
                            if(ref != null && refMap.remove(tag, ref)) {
                                ref.assign(tagMsg.msg());
                            }
                        }
                    });
                }
                if(entityList.size() > MAX_LIST_SIZE) {
                    entityList = new ArrayList<>();
                }else {
                    entityList.clear();
                }
            }
            return readBuffer.currentIndex();
        }

        private void close() {
            if(nodeMap.remove(channel.socket().intValue(), this)) {
                if (tempBuffer != null) {
                    tempBuffer.close();
                    tempBuffer = null;
                }
                if (refMap != null) {
                    refMap.forEach(ref -> ref.assign(Channel.FAILED));
                }
                if (seqRef != null) {
                    seqRef.assign(Channel.FAILED);
                }
                int state = mutex.pLock();
                try {
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
                    state |= Constants.NET_PC;
                } finally {
                    mutex.pUnlock(state);
                }
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

    /**
     *   Segment map is a data structure used in communication between virtual threads and platform threads with memory uniqueness mapping
     *   The design was to sacrifice some performance for more robust error checking mechanism, based on sorted linked list
     */
    final class RefMap {
        private final Node[] nodes;
        private final int mask;
        private int count;

        public static RefMap newInstance(int size) {
            if(Integer.bitCount(size) != 1) {
                throw new FrameworkException(ExceptionType.CONTEXT, "Size must be power of 2");
            }
            return new RefMap(size);
        }

        private RefMap(int size) {
            this.nodes = new Node[size];
            this.mask = size - 1;
            this.count = 0;
        }

        private static final class Node {
            int prefix;
            MemorySegment seg;
            Ref ref;
            Node next;
        }

        private static int getPrefix(MemorySegment segment) {
            long len = segment.byteSize();
            if(len >= Integer.BYTES) {
                return NativeUtil.getInt(segment, 0L);
            } else if(len >= Short.BYTES) {
                return NativeUtil.getShort(segment, 0L);
            } else if(len >= Byte.BYTES) {
                return NativeUtil.getByte(segment, 0L);
            } else {
                throw new FrameworkException(ExceptionType.CONTEXT, "segment is empty");
            }
        }

        public Ref get(MemorySegment segment) {
            if(NativeUtil.checkNullPointer(segment) || segment.isNative()) {
                throw new FrameworkException(ExceptionType.CONTEXT, Constants.UNREACHED);
            }
            int prefix = getPrefix(segment);
            int index = prefix & mask;
            Node cur = nodes[index];
            while(cur != null) {
                if(cur.prefix == prefix) {
                    if(cur.seg.mismatch(segment) < 0) {
                        return cur.ref;
                    }else {
                        cur = cur.next;
                    }
                } else if(cur.prefix > prefix) {
                    return null;
                } else {
                    cur = cur.next;
                }
            }
            return null;
        }

        public void put(MemorySegment segment, Ref ref) {
            if(NativeUtil.checkNullPointer(segment) || segment.isNative() || ref == null) {
                throw new FrameworkException(ExceptionType.NETWORK, Constants.UNREACHED);
            }
            int prefix = getPrefix(segment);
            Node n = new Node();
            n.prefix = prefix;
            n.seg = segment;
            n.ref = ref;
            int index = prefix & mask;
            Node p = null, cur = nodes[index];
            if(cur == null) {
                nodes[index] = n;
                count++;
                return ;
            }
            while (cur != null && cur.prefix <= prefix) {
                if(cur.prefix == prefix && cur.seg.mismatch(segment) < 0) {
                    throw new FrameworkException(ExceptionType.NETWORK, "Same segment found");
                }
                p = cur;
                cur = cur.next;
            }
            if(p == null) {
                nodes[index] = n;
                n.next = cur;
            }else if(cur == null) {
                p.next = n;
            }else {
                p.next = n;
                n.next = cur;
            }
            count++;
        }

        public boolean remove(MemorySegment segment, Ref ref) {
            if(NativeUtil.checkNullPointer(segment) || segment.isNative() || ref == null) {
                throw new FrameworkException(ExceptionType.CONTEXT, Constants.UNREACHED);
            }
            int prefix = getPrefix(segment);
            int index = prefix & mask;
            Node p = null, cur = nodes[index];
            while (cur != null) {
                if(cur.prefix == prefix && cur.seg.mismatch(segment) < 0) {
                    if(p == null) {
                        nodes[index] = cur.next;
                    }else {
                        p.next = cur.next;
                    }
                    count--;
                    return true;
                }else if(cur.prefix > prefix) {
                    return false;
                }else {
                    p = cur;
                    cur = cur.next;
                }
            }
            return false;
        }

        public boolean isEmpty() {
            return count == 0;
        }

        public int count() {
            return count;
        }

        public void forEach(Consumer<Ref> refConsumer) {
            if(count > 0) {
                for (Node n : nodes) {
                    Node ptr = n;
                    while (ptr != null) {
                        refConsumer.accept(ptr.ref);
                        ptr = ptr.next;
                    }
                }
            }
        }

    }
}
