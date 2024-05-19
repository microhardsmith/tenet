package cc.zorcc.core;

import java.lang.invoke.MethodHandles;
import java.lang.invoke.VarHandle;
import java.util.concurrent.locks.LockSupport;

/**
 *   Ref is a data structure for communication between virtual threads and platform threads, it's similar to CompletableFuture<Object>, without interruption support
 *   The typical use cases are virtual thread passing a Ref to a platform thread by a concurrent queue, then blocking wait for result
 *   Ref should never be reused or pooled
 */
public final class Ref {
    private static final VarHandle handle;

    private static final Object DEFAULT = new Object();

    private static final Object CONSUMED = new Object();

    @SuppressWarnings("FieldMayBeFinal")
    private volatile Object obj = DEFAULT;

    static {
        try{
            MethodHandles.Lookup lookup = MethodHandles.lookup();
            handle = lookup.findVarHandle(Ref.class, "obj", Object.class);
        }catch (ReflectiveOperationException e) {
            throw new ExceptionInInitializerError(e);
        }
    }

    private Object getVolatileValue() {
        try {
            return handle.getVolatile(this);
        } catch (Throwable e) {
            throw new RuntimeException(Constants.UNREACHED, e);
        }
    }

    private boolean cas(Object expectedValue, Object newValue) {
        try {
            return handle.compareAndSet(this, expectedValue, newValue);
        } catch (Throwable e) {
            throw new RuntimeException(Constants.UNREACHED, e);
        }
    }

    /**
     *   Blocking wait for result
     *   This function may block forever to wait for state change, so there must be a mechanism to release the caller thread
     */
    public Object fetch() {
        Thread currentThread = Thread.currentThread();
        if(!currentThread.isVirtual()) {
            throw new FrameworkException(ExceptionType.MISUSE, Constants.UNREACHED);
        }
        for( ; ; ) {
            Object current = getVolatileValue();
            if(current == DEFAULT) {
                if(cas(current, currentThread)) {
                    // now we wait for the producer to unpark us
                    LockSupport.park();
                }
            }else if(current == currentThread) {
                // could be a suspicious wakeup, just park
                LockSupport.park();
            }else if(current == CONSUMED) {
                throw new FrameworkException(ExceptionType.MISUSE, "Ref couldn't be reused");
            }else {
                if(cas(current, CONSUMED)) {
                    return current;
                }else {
                    throw new FrameworkException(ExceptionType.MISUSE, "Multiple fetcher detected, which should never happen");
                }
            }
        }
    }

    /**
     *   Used by producer thread to assign the result to consumer, this function should be ensured to be called, unless the consumer would be forever waiting
     *   Multiple producer might invoke this function at the same time, the first one would succeed, the following would have no side effect
     */
    public void assign(Object result) {
        if(result instanceof Thread) {
            throw new FrameworkException(ExceptionType.MISUSE, "Could not assign thread to ref as result");
        }
        for( ; ; ) {
            Object current = getVolatileValue();
            if(current == DEFAULT) {
                if(cas(current, result)) {
                    // successfully assigned
                    return ;
                }
            }else if(current instanceof Thread callerThread) {
                if(cas(current, result)) {
                    // successfully assigned, wakeup the caller thread
                    LockSupport.unpark(callerThread);
                    return ;
                }
            }else {
                // Could be assigned by other thread, or already consumed, we won't overwrite it
                return ;
            }
        }
    }
}
