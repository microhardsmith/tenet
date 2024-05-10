package cn.zorcc.common;

import cn.zorcc.common.exception.FrameworkException;

import java.lang.invoke.MethodHandles;
import java.lang.invoke.VarHandle;
import java.util.concurrent.locks.LockSupport;

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

    private Object get() {
        try {
            return handle.get(this);
        } catch (Throwable e) {
            throw new FrameworkException(ExceptionType.CONTEXT, Constants.UNREACHED, e);
        }
    }

    private boolean cas(Object expectedValue, Object newValue) {
        try {
            return handle.compareAndSet(this, expectedValue, newValue);
        } catch (Throwable e) {
            throw new FrameworkException(ExceptionType.CONTEXT, Constants.UNREACHED, e);
        }
    }

    /**
     *   Blocking wait for result
     *   This function may block forever to wait for state change, so there must be a mechanism to release the caller thread
     */
    public Object fetch() {
        Thread currentThread = Thread.currentThread();
        if(!currentThread.isVirtual()) {
            throw new FrameworkException(ExceptionType.CONTEXT, Constants.UNREACHED);
        }
        for( ; ; ) {
            Object current = get();
            if(current == DEFAULT) {
                if(cas(current, currentThread)) {
                    LockSupport.park();
                }
            }else if(current == currentThread) {
                LockSupport.park();
            }else if(current == CONSUMED) {
                throw new FrameworkException(ExceptionType.CONTEXT, "Ref couldn't be reused");
            }else {
                if(cas(current, CONSUMED)) {
                    return current;
                }else {
                    throw new FrameworkException(ExceptionType.CONTEXT, "Multiple fetcher detected, which should not happen");
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
            throw new FrameworkException(ExceptionType.CONTEXT, "Could not assign thread to ref as result");
        }
        for( ; ; ) {
            Object current = get();
            if(current == DEFAULT) {
                if(cas(current, result)) {
                    return ;
                }
            }else if(current instanceof Thread callerThread) {
                if(cas(current, result)) {
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
