package cn.zorcc.common.network;

import cn.zorcc.common.Carrier;
import cn.zorcc.common.Constants;
import cn.zorcc.common.ReadBuffer;
import cn.zorcc.common.WriteBuffer;
import cn.zorcc.common.enums.ExceptionType;
import cn.zorcc.common.exception.FrameworkException;
import cn.zorcc.common.log.Logger;
import cn.zorcc.common.structure.IntMap;
import cn.zorcc.common.structure.IntMapNode;

import java.lang.foreign.Arena;
import java.lang.foreign.MemorySegment;
import java.time.Duration;
import java.util.concurrent.locks.LockSupport;

/**
 *   Receiver is bind to a connection in its reader thread, all the operations that modifies the receiver should happen in reader thread
 */
public final class Receiver {
    private static final Logger log = new Logger(Receiver.class);
    /**
     *   Global receiverMap for its reader thread
     */
    private final IntMap<Receiver> receiverMap;
    /**
     *   Connector instance for acceptor
     */
    private Connector connector;
    /**
     *   Protocol instance for channel
     */
    private Protocol protocol;
    /**
     *   Channel instance, could be null if still at acceptor phase
     */
    private Channel channel;
    /**
     *   Store pending taggedMsg, could be null if currently not initialized
     */
    private IntMap<TaggedMsg> tagMap;
    /**
     *   Last synchronous msg tag
     */
    private int lastTag = 0;
    /**
     *   Store the buffer that can't be resolved by decoder at present
     */
    private WriteBuffer tempBuffer;

    public Receiver(IntMap<Receiver> receiverMap) {
        this.receiverMap = receiverMap;
    }

    public IntMap<Receiver> getReceiverMap() {
        return receiverMap;
    }

    public Connector getConnector() {
        return connector;
    }

    public void setConnector(Connector connector) {
        this.connector = connector;
    }

    public Protocol getProtocol() {
        return protocol;
    }

    public void setProtocol(Protocol protocol) {
        this.protocol = protocol;
    }

    public Channel getChannel() {
        return channel;
    }

    public void setChannel(Channel channel) {
        this.channel = channel;
    }

    public IntMap<TaggedMsg> getTagMap() {
        return tagMap;
    }

    public void setTagMap(IntMap<TaggedMsg> tagMap) {
        this.tagMap = tagMap;
    }

    public int getLastTag() {
        return lastTag;
    }

    public void setLastTag(int lastTag) {
        this.lastTag = lastTag;
    }

    public WriteBuffer getTempBuffer() {
        return tempBuffer;
    }

    public void setTempBuffer(WriteBuffer tempBuffer) {
        this.tempBuffer = tempBuffer;
    }

    /**
     *   Handling the incoming data, this function should only be used in Protocol implementations
     */
    public void onChannelBuffer(ReadBuffer buffer) {
        if(tempBuffer != null) {
            tempBuffer.writeSegment(buffer.rest());
            ReadBuffer readBuffer = new ReadBuffer(tempBuffer.content());
            receive(readBuffer);
            if(readBuffer.readIndex() < readBuffer.size()) {
                tempBuffer = tempBuffer.truncate(readBuffer.readIndex());
            }else {
                tempBuffer.close();
                tempBuffer = null;
            }
        }else {
            receive(buffer);
            if(buffer.readIndex() < buffer.size()) {
                tempBuffer = WriteBuffer.newDefaultWriteBuffer(Arena.ofConfined(), buffer.size());
                tempBuffer.writeSegment(buffer.rest());
            }
        }
    }

    /**
     *   Actually receive data from the readBuffer, unpark the waiter thread if necessary
     */
    private void receive(ReadBuffer buffer) {
        try{
            Object result = channel.decoder().decode(buffer);
            if(result != null) {
                int tag = calculateTag(channel.handler().onRecv(channel, result));
                if(tag != 0) {
                    IntMapNode<TaggedMsg> node = tagMap.getNode(tag);
                    if(node != null) {
                        Carrier carrier = node.getValue().carrier();
                        if(carrier.target().compareAndSet(Carrier.HOLDER, result)) {
                            tagMap.removeNode(tag, node);
                            LockSupport.unpark(carrier.thread());
                        }
                    }
                }
            }
        }catch (FrameworkException e) {
            log.error(STR."Failed to perform reading from channel : \{channel.loc()}", e);
            channel.shutdown();
        }
    }

    /**
     *   Calculate tag based on the return value of onRecv()
     */
    private int calculateTag(int r) {
        if(r >= 0) {
            return r;
        }else {
            int i = lastTag;
            lastTag = 0;
            return i;
        }
    }

    /**
     *   When multiplexing read event triggered, this function should be called to handle it
     */
    public void doRead(MemorySegment reserved) {
        if(connector != null) {
            connector.canRead(reserved);
        }else if(protocol != null) {
            protocol.canRead(reserved);
        }else {
            throw new FrameworkException(ExceptionType.NETWORK, Constants.UNREACHED);
        }
    }

    /**
     *   When multiplexing write event triggered, this function should be called to handle it
     */
    public void doWrite() {
        if(connector != null) {
            connector.canWrite();
        }else if(protocol != null) {
            protocol.canWrite();
        }else {
            throw new FrameworkException(ExceptionType.NETWORK, Constants.UNREACHED);
        }
    }

    /**
     *   Shutdown underlying acceptor or channel
     */
    public void gracefulShutdown(Duration duration) {
        if(connector != null) {
            close();
        }else if(channel != null) {
            channel.shutdown(duration);
        }else {
            throw new FrameworkException(ExceptionType.NETWORK, Constants.UNREACHED);
        }
    }

    /**
     *   Close underlying channel
     */
    public void close() {
        if(channel == null) {
            throw new FrameworkException(ExceptionType.NETWORK, Constants.UNREACHED);
        }
        Socket socket = channel.socket();
        Worker worker = channel.worker();
        if(receiverMap.remove(socket.intValue(), this)) {
            int current = channel.state().getAndSet(OsNetworkLibrary.REGISTER_NONE);
            if(current > OsNetworkLibrary.REGISTER_NONE) {
                OsNetworkLibrary.CURRENT.ctl(worker.mux(), socket, current, OsNetworkLibrary.REGISTER_NONE);
            }
            if(connector != null) {
                connector.doClose();
            }else if(protocol != null) {
                worker.submitWriterTask(new WriterTask(WriterTask.WriterTaskType.REMOVE, channel, null, null));
                protocol.doClose();
                channel.handler().onRemoved(channel);
            }else {
                throw new FrameworkException(ExceptionType.NETWORK, Constants.UNREACHED);
            }
            if (worker.counter().decrementAndGet() == 0) {
                worker.submitReaderTask(ReaderTask.POSSIBLE_SHUTDOWN_TASK);
            }
        }
    }

    /**
     *   Upgrade current Acceptor to a new created Channel
     *   Note that this function should only be invoked by its connector in shouldRead() or shouldWrite() to replace current Acceptor.
     */
    public void upgradeToChannel() {
        if(acceptor == null) {
            throw new FrameworkException(ExceptionType.NETWORK, Constants.UNREACHED);
        }
        channel = new Channel(acceptor);
        acceptor = null;
        channel.worker().submitWriterTask(new WriterTask(WriterTask.WriterTaskType.INITIATE, channel, null, null));
        int from = channel.state().getAndSet(OsNetworkLibrary.REGISTER_READ);
        if(from != OsNetworkLibrary.REGISTER_READ) {
            OsNetworkLibrary.CURRENT.ctl(channel.worker().mux(), channel.socket(), from, OsNetworkLibrary.REGISTER_READ);
        }
        channel.handler().onConnected(channel);
    }
}
