package cn.zorcc.common.structure;

import cn.zorcc.common.Constants;
import cn.zorcc.common.ExceptionType;
import cn.zorcc.common.exception.FrameworkException;

import java.lang.invoke.MethodHandles;
import java.lang.invoke.VarHandle;
import java.util.function.LongFunction;
import java.util.function.LongUnaryOperator;

/**
 *   LongHolder is just a simple long value, which could be modified with different access modes
 */
@SuppressWarnings("unused")
public final class LongHolder {
    private static final VarHandle handle;
    static {
        try {
            MethodHandles.Lookup lookup = MethodHandles.lookup();
            handle = lookup.findVarHandle(LongHolder.class, "value", long.class).withInvokeExactBehavior();
        } catch (ReflectiveOperationException e) {
            throw new FrameworkException(ExceptionType.CONTEXT, Constants.UNREACHED);
        }
    }

    private long value;

    public LongHolder(long initialValue) {
        this.value = initialValue;
    }

    public void increment() {
        value++;
    }

    public void decrement() {
        value--;
    }

    public long getValue() {
        return (long) handle.get(this);
    }

    public void setValue(long newValue) {
        handle.set(this, newValue);
    }

    public long getVolatileValue() {
        return (long) handle.getVolatile(this);
    }

    public void setVolatileValue(long newValue) {
        handle.setVolatile(this, newValue);
    }

    public long getAcquireValue() {
        return (long) handle.getAcquire(this);
    }

    public void setReleaseValue(long newValue) {
        handle.setRelease(this, newValue);
    }

    public boolean casValue(long oldValue, long newValue) {
        return handle.compareAndSet(this, oldValue, newValue);
    }

    public long getAndSet(long newValue) {
        return (long) handle.getAndSet(this, newValue);
    }

    public long getAndUpdate(LongUnaryOperator operator) {
        for( ; ; ) {
            long current = getVolatileValue();
            long next = operator.applyAsLong(current);
            if(casValue(current, next)) {
                return current;
            }else {
                Thread.onSpinWait();
            }
        }
    }

    public long updateAndGet(LongUnaryOperator operator) {
        for( ; ; ) {
            long current = getVolatileValue();
            long next = operator.applyAsLong(current);
            if(casValue(current, next)) {
                return next;
            }else {
                Thread.onSpinWait();
            }
        }
    }

    private static final long LOCK_VALUE = Long.MIN_VALUE;

    /**
     *   Acquiring a long lock, note that this function is not reentrant, acquiring a lock multiple times will cause deadlock
     */
    public long lock(Runnable waitOp) {
        for( ; ; ) {
            long current = getVolatileValue();
            if(current == LOCK_VALUE) {
                waitOp.run();
            }else if(casValue(current, LOCK_VALUE)) {
                return current;
            }else {
                waitOp.run();
            }
        }
    }

    /**
     *   Pair with lock()
     */
    public void unlock(long next) {
        if(next == LOCK_VALUE || !casValue(LOCK_VALUE, next)) {
            throw new FrameworkException(ExceptionType.CONTEXT, Constants.UNREACHED);
        }
    }

    /**
     *   Transform current LongHolder to another state without lock acquired
     */
    public void transform(LongUnaryOperator operator, Runnable waitOp) {
        long r = lock(waitOp);
        try {
            r = operator.applyAsLong(r);
        } finally {
            unlock(r);
        }
    }

    /**
     *   Extract current LongHolder's value without state change or lock
     */
    public <T> T extract(LongFunction<T> func, Runnable waitOp) {
        long r = lock(waitOp);
        try{
            return func.apply(r);
        }finally {
            unlock(r);
        }
    }

}
