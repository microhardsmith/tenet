package cn.zorcc.common.jmh;

import cn.zorcc.common.Constants;
import cn.zorcc.common.ExceptionType;
import cn.zorcc.common.exception.FrameworkException;
import cn.zorcc.common.network.*;
import cn.zorcc.common.structure.Allocator;
import cn.zorcc.common.structure.IntHolder;
import cn.zorcc.common.structure.MemApi;
import cn.zorcc.common.util.NativeUtil;
import org.openjdk.jmh.annotations.Benchmark;
import org.openjdk.jmh.annotations.Param;
import org.openjdk.jmh.runner.RunnerException;

import java.lang.foreign.MemorySegment;
import java.lang.foreign.ValueLayout;
import java.lang.invoke.VarHandle;
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
        void unlock(int current, int next);
    }

    @Param({"2", "10", "100"})
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
                        int current = lock.lock();
                        int next;
                        if(current == Constants.NET_NONE) {
                            OsNetworkLibrary.CURRENT.ctlMux(mux, socket, Constants.NET_NONE, Constants.NET_R, MemApi.DEFAULT);
                            next = Constants.NET_R;
                        }else if(current == Constants.NET_R) {
                            OsNetworkLibrary.CURRENT.ctlMux(mux, socket, Constants.NET_R, Constants.NET_NONE, MemApi.DEFAULT);
                            next = Constants.NET_NONE;
                        }else {
                            throw new FrameworkException(ExceptionType.NETWORK, Constants.UNREACHED);
                        }
                        unlock.unlock(current, next);
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
        }, (_, b) -> {
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
    public void testSpinLockWithPadding() {
        IntPaddingHolder holder = new IntPaddingHolder(Constants.NET_NONE);
        testLock(() -> holder.lock(Thread::onSpinWait), holder::unlock);
    }

    @Benchmark
    public void testYieldLock() {
        IntHolder holder = new IntHolder(Constants.NET_NONE);
        testLock(() -> holder.lock(Thread::yield), holder::unlock);
    }

    @Benchmark
    public void testYieldLockWithPadding() {
        IntPaddingHolder holder = new IntPaddingHolder(Constants.NET_NONE);
        testLock(() -> holder.lock(Thread::yield), holder::unlock);
    }


    public static void main(String[] args) throws RunnerException {
        runTest(SpinLockTest.class);
    }

    record IntPaddingHolder(
            MemorySegment memorySegment
    ) {
        private static final long PADDING_SIZE = 64;
        private static final VarHandle handle = ValueLayout.JAVA_INT.varHandle().withInvokeExactBehavior();
        public IntPaddingHolder(int initialValue) {
            this(Allocator.HEAP.allocate(PADDING_SIZE * 2 + Integer.BYTES));
            NativeUtil.setInt(memorySegment, PADDING_SIZE, initialValue);
        }

        public int getAndBitwiseOr(int orValue) {
            return (int) handle.getAndBitwiseOr(memorySegment, PADDING_SIZE, orValue);
        }

        public int getAndBitwiseXor(int xorValue) {
            return (int) handle.getAndBitwiseXor(memorySegment, PADDING_SIZE, xorValue);
        }
        private static final int MASK = 1 << 31;
        public int lock(Runnable waitOp) {
            for( ; ; ) {
                int current = getAndBitwiseOr(MASK);
                if((current & MASK) != 0) {
                    waitOp.run();
                }else {
                    return current;
                }
            }
        }

        public void unlock(int current, int next) {
            if ((getAndBitwiseXor((current ^ next) | MASK) & MASK) == 0) {
                throw new FrameworkException(ExceptionType.CONTEXT, Constants.UNREACHED);
            }
        }
    }
}
