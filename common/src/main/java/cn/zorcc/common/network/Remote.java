package cn.zorcc.common.network;

import cn.zorcc.common.Clock;
import cn.zorcc.common.Constants;
import cn.zorcc.common.ObjPool;
import cn.zorcc.common.exception.NetworkException;
import cn.zorcc.common.pojo.Loc;
import cn.zorcc.common.wheel.Job;
import cn.zorcc.common.wheel.Wheel;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.LinkedTransferQueue;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TransferQueue;
import java.util.concurrent.atomic.AtomicInteger;

/**
 *   Abstraction for current tcp client's connection with remote server as a connection pool
 *   Remote instance is unique for a single Loc, and it doesn't take over the channel management
 */
public class Remote implements ObjPool<Channel> {
    private static final Wheel wheel = Wheel.wheel();
    private static final Map<Loc, Remote> remoteMap = new ConcurrentHashMap<>();

    /**
     *   Remote's location
     */
    private final Loc loc;
    /**
     *   current available channels
     */
    private final TransferQueue<Channel> channels = new LinkedTransferQueue<>();
    /**
     *   available channel count
     */
    private final AtomicInteger counter = new AtomicInteger(Constants.ZERO);
    private Remote(Loc loc) {
        this.loc = loc;
    }

    /**
     *   creating a newly created Remote instance
     *   when using Net for client connecting, first obtain a Remote instance by this method, then calling connect to establish a new connection
     *   the newly created connection will be automatically added to the Remote instance
     */
    public static Remote remote(Loc loc) {
        return remoteMap.computeIfAbsent(loc, Remote::new);
    }

    public Loc loc() {
        return loc;
    }

    @Override
    public Channel get() {
        Thread currentThread = Thread.currentThread();
        try{
            Channel ch = channels.take();
            if(!ch.isAvailable()) {
                counter.decrementAndGet();
                return get();
            }
            return ch;
        } catch (InterruptedException e) {
            currentThread.interrupt();
            throw new NetworkException(NetworkException.CONNECTION_TIME_OUT);
        }
    }

    @Override
    public Channel get(long timeout, TimeUnit timeUnit) {
        long nano = timeUnit.toNanos(timeout);
        Thread currentThread = Thread.currentThread();
        try{
            Channel ch = channels.poll();
            if(ch == null) {
                // Current queue is empty, schedule a new one
                long start = Clock.nano();
                Job timeoutJob = wheel.addJob(currentThread::interrupt, timeout, timeUnit);
                ch = channels.take();
                if (!timeoutJob.cancel()) {
                    // failed to cancel the interrupt
                    throw new NetworkException(NetworkException.CONNECTION_TIME_OUT);
                }
                nano -= Clock.elapsed(start);
            }
            if(!ch.isAvailable()) {
                counter.decrementAndGet();
                return get(nano, TimeUnit.NANOSECONDS);
            }
            return ch;
        } catch (InterruptedException e) {
            currentThread.interrupt();
            throw new NetworkException(NetworkException.CONNECTION_TIME_OUT);
        }
    }

    @Override
    public void release(Channel channel) {
        if(channel.isAvailable()) {
            if (!channels.offer(channel)) {
                throw new NetworkException(Constants.UNREACHED);
            }
        }
    }

    @Override
    public void add(Channel channel) {
        release(channel);
        counter.getAndIncrement();
    }

    @Override
    public int count() {
        return counter.get();
    }
}
