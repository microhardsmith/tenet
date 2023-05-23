package cn.zorcc.common.network;

import cn.zorcc.common.Clock;
import cn.zorcc.common.Constants;
import cn.zorcc.common.ObjPool;
import cn.zorcc.common.enums.ExceptionType;
import cn.zorcc.common.exception.FrameworkException;
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
    private static final String CONNECTION_TIME_OUT = "Acquire connection timeout";
    private static final Wheel wheel = Wheel.wheel();
    /**
     *   global remote map
     */
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
     *   available channel count, when channel is actually put into channels, the availableCounter will increment
     */
    private final AtomicInteger availableCounter = new AtomicInteger(Constants.ZERO);
    /**
     *   connect call times, when Net.connect() is called, the connectCounter will increment
     */
    private final AtomicInteger connectCounter = new AtomicInteger(Constants.ZERO);
    /**
     *   permitted maximum connections
     */
    private volatile int maximum = 1;

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

    public AtomicInteger connectCounter() {
        return connectCounter;
    }

    public void setMaximum(int maximum) {
        this.maximum = maximum;
    }

    public int getMaximum() {
        return maximum;
    }

    public Loc loc() {
        return loc;
    }

    @Override
    public Channel get() {
        Thread currentThread = Thread.currentThread();
        try{
            Channel ch = channels.take();
            if(!ch.available()) {
                availableCounter.decrementAndGet();
                connectCounter.decrementAndGet();
                return get();
            }
            return ch;
        } catch (InterruptedException e) {
            currentThread.interrupt();
            throw new FrameworkException(ExceptionType.NETWORK, CONNECTION_TIME_OUT);
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
                    throw new FrameworkException(ExceptionType.NETWORK, CONNECTION_TIME_OUT);
                }
                nano -= Clock.elapsed(start);
            }
            if(!ch.available()) {
                availableCounter.decrementAndGet();
                connectCounter.decrementAndGet();
                return get(nano, TimeUnit.NANOSECONDS);
            }
            return ch;
        } catch (InterruptedException e) {
            currentThread.interrupt();
            throw new FrameworkException(ExceptionType.NETWORK, CONNECTION_TIME_OUT);
        }
    }

    @Override
    public void release(Channel channel) {
        if(channel.available()) {
            if (!channels.offer(channel)) {
                throw new FrameworkException(ExceptionType.NETWORK, Constants.UNREACHED);
            }
        }
    }

    @Override
    public void add(Channel channel) {
        release(channel);
        availableCounter.getAndIncrement();
    }

    @Override
    public int count() {
        return availableCounter.get();
    }
}
