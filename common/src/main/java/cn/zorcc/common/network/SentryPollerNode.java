package cn.zorcc.common.network;

import cn.zorcc.common.Constants;
import cn.zorcc.common.ExceptionType;
import cn.zorcc.common.State;
import cn.zorcc.common.exception.FrameworkException;
import cn.zorcc.common.log.Logger;
import cn.zorcc.common.network.api.Protocol;
import cn.zorcc.common.network.api.Sentry;
import cn.zorcc.common.network.lib.OsNetworkLibrary;
import cn.zorcc.common.structure.IntMap;

import java.lang.foreign.MemorySegment;
import java.time.Duration;

public final class SentryPollerNode implements PollerNode {
    private static final Logger log = new Logger(SentryPollerNode.class);
    private static final OsNetworkLibrary osNetworkLibrary = OsNetworkLibrary.CURRENT;
    private final IntMap<PollerNode> nodeMap;
    private final Channel channel;
    private final Sentry sentry;
    private final State channelState = new State(Constants.NET_W);
    private final Runnable callback;

    public SentryPollerNode(IntMap<PollerNode> nodeMap, Channel channel, Sentry sentry, Runnable callback) {
        this.nodeMap = nodeMap;
        this.channel = channel;
        this.sentry = sentry;
        this.callback = callback;
    }

    @Override
    public void onReadableEvent(MemorySegment reserved, int len) {
        try{
            handleEvent(sentry.onReadableEvent(reserved, len));
        }catch (FrameworkException e) {
            log.error("Exception thrown in sentryPollerNode when invoking onReadableEvent()", e);
            close();
        }
    }

    @Override
    public void onWritableEvent() {
        try {
            handleEvent(sentry.onWritableEvent());
        }catch (FrameworkException e) {
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
        if(r == Constants.NET_UPDATE) {
            updateToProtocol();
        }else if(r == Constants.NET_R || r == Constants.NET_W || r == Constants.NET_RW){
            ctl(r);
        }else if(r != Constants.NET_IGNORED) {
            throw new FrameworkException(ExceptionType.NETWORK, Constants.UNREACHED);
        }
    }

    private void ctl(int expected) {
        int current = channelState.get();
        if(current != expected) {
            osNetworkLibrary.ctl(channel.poller().mux(), channel.socket(), current, expected);
            channelState.set(expected);
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
        ProtocolPollerNode protocolPollerNode = new ProtocolPollerNode(nodeMap, channel, protocol, channelState);
        nodeMap.replace(channel.socket().intValue(), this, protocolPollerNode);
        channel.writer().submit(new WriterTask(WriterTaskType.INITIATE, channel, new ProtoAndState(protocol, channelState), null));
    }

    private void close() {
        if(nodeMap.remove(channel.socket().intValue(), this)) {
            closeSentry();
        }
    }

    private void closeSentry() {
        try{
            sentry.doClose();
        }catch (RuntimeException e) {
            log.error("Failed to close sentry", e);
        }
        if(callback != null) {
            Thread.ofVirtual().start(callback);
        }
    }
}
