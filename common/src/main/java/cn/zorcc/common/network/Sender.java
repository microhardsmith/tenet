package cn.zorcc.common.network;

import cn.zorcc.common.Constants;
import cn.zorcc.common.WriteBuffer;
import cn.zorcc.common.enums.ExceptionType;
import cn.zorcc.common.exception.FrameworkException;
import cn.zorcc.common.log.Logger;
import cn.zorcc.common.structure.IntMap;
import cn.zorcc.common.structure.Wheel;

import java.lang.foreign.Arena;
import java.lang.foreign.MemorySegment;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 *   Sender represents all the data-structure that writer thread could manipulate
 */
public final class Sender {
    private record SenderTask(
            WriteBuffer writeBuffer,
            WriterCallback callback
    ){

    }

    private static final Logger log = new Logger(Sender.class);
    private final Worker worker;
    private final IntMap<Sender> senderMap;
    /**
     *   Whether the sender has been shutdown, normally it's null, could exist when tempBuffer is not null
     */
    private Duration duration;
    /**
     *   Tasks exist means current channel is not writable, incoming data should be wrapped into tasks list first, normally it's null
     */
    private List<SenderTask> tasks;
    
    public Sender(Worker worker, IntMap<Sender> senderMap) {
        this.worker = worker;
        this.senderMap = senderMap;
    }

    public void sendMultipleMsg(Channel channel, MemorySegment reserved, List<?> msgList, WriterCallback callback) {
        try(final WriteBuffer writeBuffer = WriteBuffer.newReservedWriteBuffer(reserved)) {
            WriteBuffer wb = writeBuffer;
            for (Object msg : msgList) {
                wb = channel.encoder().encode(wb, msg);
            }
            if(wb.writeIndex() > 0) {
                doSend(channel, wb, callback);
            }
            if(wb != writeBuffer) {
                wb.close();
            }
        }catch (FrameworkException e) {
            log.error(STR."Failed to perform writing from channel : \{channel.loc()}", e);
            channel.shutdown();
        }
    }

    public void sendMsg(Channel channel, MemorySegment reserved, Object msg, WriterCallback callback) {
        try(final WriteBuffer writeBuffer = WriteBuffer.newReservedWriteBuffer(reserved)) {
            WriteBuffer wb = channel.encoder().encode(writeBuffer, msg);
            if(wb.writeIndex() > 0) {
                doSend(channel, wb, callback);
            }
            if(wb != writeBuffer) {
                wb.close();
            }
        } catch (FrameworkException e) {
            log.error(STR."Failed to perform writing from channel : \{channel.loc()}", e);
            channel.shutdown();
        }
    }

    public void becomeWritable(Channel channel) {
        if(tasks != null) {
            for (int i = 0; i < tasks.size(); i++) {
                SenderTask st = tasks.get(i);
                switch (channel.protocol().doWrite(channel, st.writeBuffer())) {
                    case SUCCESS -> {
                        st.writeBuffer().close();
                        Optional.ofNullable(st.callback()).ifPresent(WriterCallback::onSuccess);
                    }
                    case PENDING -> {
                        return ;
                    }
                    case FAILURE -> {
                        senderMap.remove(channel.socket().intValue());
                        worker.submitReaderTask(new ReaderTask(ReaderTask.ReaderTaskType.CLOSE_ACTOR, null, channel, null, null));
                        for(int j = i; j < tasks.size(); j++) {
                            SenderTask followingSt = tasks.get(j);
                            followingSt.writeBuffer().close();
                            Optional.ofNullable(followingSt.callback()).ifPresent(WriterCallback::onFailure);
                        }
                        return ;
                    }
                }
            }
            tasks = null;
            if(duration != null) {
                doShutdown(senderMap, channel, duration);
            }
        }
    }

    public void shutdown(Channel channel, Duration d) {
        if(tasks == null) {
            doShutdown(senderMap, channel, d);
        }else if(duration == null){
            this.duration = d;
        }
    }

    public void close(Channel channel) {
        senderMap.remove(channel.socket().intValue());
        if(tasks != null) {
            for (SenderTask st : tasks) {
                st.writeBuffer().close();
                Optional.ofNullable(st.callback()).ifPresent(WriterCallback::onFailure);
            }
        }
    }

    /**
     *   Try to send writeBuffer, will write into tempBuffer if current channel is not writable
     *   Note that since the whole operation is single-threaded, shutdown will only exist when tempBuffer is not null, so here we only need to examine the tempBuffer
     */
    private void doSend(Channel channel, WriteBuffer writeBuffer, WriterCallback callback) {
        if(tasks == null) {
            switch (channel.protocol().doWrite(channel, writeBuffer)) {
                case SUCCESS -> {
                    if(callback != null) {
                        callback.onSuccess();
                    }
                }
                case PENDING -> {
                    tasks = new ArrayList<>();
                    copyLocally(writeBuffer, callback);
                }
                case FAILURE -> {
                    senderMap.remove(channel.socket().intValue());
                    worker.submitReaderTask(new ReaderTask(ReaderTask.ReaderTaskType.CLOSE_ACTOR, null, channel, null, null));
                    if(callback != null) {
                        callback.onFailure();
                    }
                }
            }
        }else {
            copyLocally(writeBuffer, callback);
        }
    }

    /**
     *   Copy target writeBuffer with its callback locally
     */
    private void copyLocally(WriteBuffer writeBuffer, WriterCallback callback) {
        Arena arena = Arena.ofConfined();
        WriteBuffer wb = WriteBuffer.newFixedWriteBuffer(arena, writeBuffer.size());
        wb.writeSegment(writeBuffer.toSegment());
        tasks.add(new SenderTask(wb, callback));
    }

    /**
     *   Perform the actual shutdown operation, no longer receiving write tasks
     */
    private void doShutdown(IntMap<Sender> senderMap, Channel channel, Duration duration) {
        if (!senderMap.remove(channel.socket().intValue(), this)) {
            throw new FrameworkException(ExceptionType.NETWORK, Constants.UNREACHED);
        }
        channel.protocol().doShutdown(channel);
        Wheel.wheel().addJob(() -> worker.submitReaderTask(new ReaderTask(ReaderTask.ReaderTaskType.CLOSE_ACTOR, null, channel, null, null)), duration);
    }
}
