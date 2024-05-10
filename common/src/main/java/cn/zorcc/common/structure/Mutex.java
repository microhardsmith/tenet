package cn.zorcc.common.structure;

import cn.zorcc.common.Constants;
import cn.zorcc.common.ExceptionType;
import cn.zorcc.common.exception.FrameworkException;

import java.lang.invoke.MethodHandles;
import java.lang.invoke.VarHandle;
import java.util.concurrent.locks.LockSupport;

/**
 *   Mut is a dedicated Mutex for state manipulation between exactly two threads, comparing to ReentrantLock, this implementation has completely no allocation
 */
public final class Mutex {
    private final Thread pT;
    private final Thread wT;
    @SuppressWarnings("FieldMayBeFinal")
    private volatile int state;
    private static final VarHandle stateHandle;

    /**
     *   Default spin count, quit spinning and park when reaching the limit
     */
    private static final int SPIN_COUNT = 64;

    private static final int P_ACQUIRE = -1;
    private static final int W_ACQUIRE = -2;
    private static final int PW_ACQUIRE = -3;
    private static final int WP_ACQUIRE = -4;

    static {
        try{
            MethodHandles.Lookup lookup = MethodHandles.lookup();
            stateHandle = lookup.findVarHandle(Mutex.class, "state", int.class);
        }catch (ReflectiveOperationException e) {
            throw new ExceptionInInitializerError(e);
        }
    }

    /**
     *   Defensive checking caller thread, could be removed
     */
    private static void checkCallerThread(Thread expected) {
        if(Thread.currentThread() != expected) {
            throw new FrameworkException(ExceptionType.CONTEXT, Constants.UNREACHED);
        }
    }

    public int getVolatileState() {
        try{
            return (int) stateHandle.getVolatile(this);
        } catch (Throwable e) {
            throw new FrameworkException(ExceptionType.CONTEXT, Constants.UNREACHED, e);
        }
    }

    private boolean casState(int expectedValue, int newValue) {
        try{
            return stateHandle.compareAndSet(this, expectedValue, newValue);
        } catch (Throwable e) {
            throw new FrameworkException(ExceptionType.CONTEXT, Constants.UNREACHED, e);
        }
    }

    public Mutex(Thread pT, Thread wT, int state) {
        if(state == P_ACQUIRE || state == W_ACQUIRE || state == PW_ACQUIRE || state == WP_ACQUIRE) {
            throw new FrameworkException(ExceptionType.CONTEXT, Constants.UNREACHED);
        }
        this.pT = pT;
        this.wT = wT;
        this.state = state;
    }

    @SuppressWarnings("Duplicates")
    public int pLock() {
        checkCallerThread(pT);
        for(int i = 0 ; ; ) {
            int current = getVolatileState();
            switch (current) {
                case P_ACQUIRE, PW_ACQUIRE -> throw new FrameworkException(ExceptionType.CONTEXT, Constants.UNREACHED);
                case W_ACQUIRE -> {
                    if(i++ < SPIN_COUNT) {
                        Thread.onSpinWait();
                    }else if(casState(current, WP_ACQUIRE)){
                        i = 0;
                        LockSupport.park();
                    }
                }
                case WP_ACQUIRE -> LockSupport.park();
                default -> {
                    if(casState(current, P_ACQUIRE)) {
                        return current;
                    }
                }
            }
        }
    }

    @SuppressWarnings("Duplicates")
    public void pUnlock(int nextValue) {
        checkCallerThread(pT);
        for( ; ; ) {
            int current = getVolatileState();
            switch (current) {
                case P_ACQUIRE -> {
                    if(casState(current, nextValue)) {
                        return ;
                    }
                }
                case PW_ACQUIRE -> {
                    if(casState(current, nextValue)) {
                        LockSupport.unpark(wT);
                        return ;
                    }
                }
                default -> throw new FrameworkException(ExceptionType.CONTEXT, Constants.UNREACHED);
            }
        }
    }

    @SuppressWarnings("Duplicates")
    public int wLock() {
        checkCallerThread(wT);
        for(int i = 0 ; ; ) {
            int current = getVolatileState();
            switch (current) {
                case W_ACQUIRE, WP_ACQUIRE -> throw new FrameworkException(ExceptionType.CONTEXT, Constants.UNREACHED);
                case P_ACQUIRE -> {
                    if(i++ < SPIN_COUNT) {
                        Thread.onSpinWait();
                    }else if(casState(current, PW_ACQUIRE)){
                        i = 0;
                        LockSupport.park();
                    }
                }
                case PW_ACQUIRE -> LockSupport.park();
                default -> {
                    if(casState(current, W_ACQUIRE)) {
                        return current;
                    }
                }
            }
        }
    }

    @SuppressWarnings("Duplicates")
    public void wUnlock(int nextValue) {
        checkCallerThread(wT);
        for( ; ; ) {
            int current = getVolatileState();
            switch (current) {
                case W_ACQUIRE -> {
                    if(casState(current, nextValue)) {
                        return ;
                    }
                }
                case WP_ACQUIRE -> {
                    if(casState(current, nextValue)) {
                        LockSupport.unpark(pT);
                        return ;
                    }
                }
                default -> throw new FrameworkException(ExceptionType.CONTEXT, Constants.UNREACHED);
            }
        }
    }
}
