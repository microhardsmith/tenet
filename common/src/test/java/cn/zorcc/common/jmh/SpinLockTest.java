package cn.zorcc.common.jmh;

import cn.zorcc.common.Constants;
import cn.zorcc.common.ExceptionType;
import cn.zorcc.common.exception.FrameworkException;
import cn.zorcc.common.network.*;
import cn.zorcc.common.structure.IntHolder;
import cn.zorcc.common.structure.LongHolder;
import cn.zorcc.common.structure.MemApi;
import org.openjdk.jmh.annotations.Benchmark;
import org.openjdk.jmh.annotations.Param;
import org.openjdk.jmh.runner.RunnerException;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.locks.ReentrantLock;

/**
 *   Conclusion : If it's just two thread, using SpinLock would be much faster,
 *   and there is no need for cache padding when accessing only a single variable
 */
public class SpinLockTest extends JmhTest {

    @FunctionalInterface
    interface LockOp {
        int lock();
    }

    @FunctionalInterface
    interface UnlockOp {
        void unlock(int next);
    }

    @Param({"2"})
    private int size;
    private static final int ITERATION = 1000;

    /**
     *   This test is targeting at multiple thread, each trying to modify the state of the Mux instance
     */
    private void testLock(LockOp lock, UnlockOp unlock) {
        CountDownLatch startSignal = new CountDownLatch(1);
        CountDownLatch endSignal = new CountDownLatch(size);
        Mux mux = OsNetworkLibrary.CURRENT.createMux();
        Socket socket = OsNetworkLibrary.CURRENT.createSocket(new Loc(IpType.IPV4, "127.0.0.1", 8080));
        for(int i = 0; i < size; i++) {
            Thread.ofPlatform().start(() -> {
                try {
                    startSignal.await();
                    for(int iter = 0; iter < ITERATION; iter++) {
                        int r = lock.lock();
                        if(r == Constants.NET_NONE) {
                            OsNetworkLibrary.CURRENT.ctlMux(mux, socket, Constants.NET_NONE, Constants.NET_R, MemApi.DEFAULT);
                            r = Constants.NET_R;
                        }else if(r == Constants.NET_R) {
                            OsNetworkLibrary.CURRENT.ctlMux(mux, socket, Constants.NET_R, Constants.NET_NONE, MemApi.DEFAULT);
                            r = Constants.NET_NONE;
                        }else {
                            throw new FrameworkException(ExceptionType.NETWORK, Constants.UNREACHED);
                        }
                        unlock.unlock(r);
                    }
                    endSignal.countDown();
                } catch (InterruptedException e) {
                    throw new RuntimeException(e);
                }
            });
        }
        startSignal.countDown();
        try {
            endSignal.await();
        } catch (InterruptedException e) {
            throw new FrameworkException(ExceptionType.NETWORK, Constants.UNREACHED);
        }
    }

    @Benchmark
    public void testReentrantLock() {
        ReentrantLock lock = new ReentrantLock();
        IntHolder holder = new IntHolder(Constants.NET_NONE);
        testLock(() -> {
            lock.lock();
            return holder.getValue();
        }, (b) -> {
            holder.setValue(b);
            lock.unlock();
        });
    }

    @Benchmark
    public void testSpinLock() {
        IntHolder holder = new IntHolder(Constants.NET_NONE);
        testLock(() -> holder.lock(Thread::onSpinWait), holder::unlock);
    }

    @Benchmark
    public void testLongSpinLock() {
        LongHolder holder = new LongHolder(Constants.NET_NONE);
        testLock(() -> (int) holder.lock(Thread::onSpinWait), holder::unlock);
    }

    @Benchmark
    public void testLongYieldLock() {
        LongHolder holder = new LongHolder(Constants.NET_NONE);
        testLock(() -> (int) holder.lock(Thread::yield), holder::unlock);
    }

    @Benchmark
    public void testYieldLock() {
        IntHolder holder = new IntHolder(Constants.NET_NONE);
        testLock(() -> holder.lock(Thread::yield), holder::unlock);
    }

    public static void main(String[] args) throws RunnerException {
        runTest(SpinLockTest.class);
    }

}
