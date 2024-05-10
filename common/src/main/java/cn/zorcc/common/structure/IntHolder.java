package cn.zorcc.common.structure;

import cn.zorcc.common.Constants;
import cn.zorcc.common.ExceptionType;
import cn.zorcc.common.exception.FrameworkException;

import java.lang.invoke.MethodHandles;
import java.lang.invoke.VarHandle;
import java.util.function.IntFunction;
import java.util.function.IntUnaryOperator;

/**
 *   IntHolder is just a simple int value, which could be modified with different access modes
 */
@SuppressWarnings("unused")
public final class IntHolder {
    private static final VarHandle handle;
    static {
        try {
            MethodHandles.Lookup lookup = MethodHandles.lookup();
            handle = lookup.findVarHandle(IntHolder.class, "value", int.class).withInvokeExactBehavior();
        } catch (ReflectiveOperationException e) {
            throw new FrameworkException(ExceptionType.CONTEXT, Constants.UNREACHED);
        }
    }
    private int value;

    public IntHolder(int initialValue) {
        this.value = initialValue;
    }

    public void increment() {
        value++;
    }

    public void decrement() {
        value--;
    }

    public int getValue() {
        return (int) handle.get(this);
    }

    public void setValue(int newValue) {
        handle.set(this, newValue);
    }

    public int getVolatileValue() {
        return (int) handle.getVolatile(this);
    }

    public void setVolatileValue(int newValue) {
        handle.setVolatile(this, newValue);
    }

    public int getAcquireValue() {
        return (int) handle.getAcquire(this);
    }

    public void setReleaseValue(int newValue) {
        handle.setRelease(this, newValue);
    }

    public boolean casValue(int oldValue, int newValue) {
        return handle.compareAndSet(this, oldValue, newValue);
    }

    public int getAndSet(int newValue) {
        return (int) handle.getAndSet(this, newValue);
    }

    public int getAndUpdate(IntUnaryOperator operator) {
        for( ; ; ) {
            int current = getVolatileValue();
            int next = operator.applyAsInt(current);
            if(casValue(current, next)) {
                return current;
            }else {
                Thread.onSpinWait();
            }
        }
    }

    public int updateAndGet(IntUnaryOperator operator) {
        for( ; ; ) {
            int current = getVolatileValue();
            int next = operator.applyAsInt(current);
            if(casValue(current, next)) {
                return next;
            }else {
                Thread.onSpinWait();
            }
        }
    }

    public int getAndBitwiseOr(int orValue) {
        return (int) handle.getAndBitwiseOr(this, orValue);
    }

    public int getAndBitwiseAnd(int andValue) {
        return (int) handle.getAndBitwiseAnd(this, andValue);
    }

    public int getAndBitwiseXor(int xorValue) {
        return (int) handle.getAndBitwiseXor(this, xorValue);
    }

    private static final int LOCK_VALUE = Integer.MIN_VALUE;

    /**
     *   Acquiring an int lock, note that this function is not reentrant, acquiring a lock multiple times will cause deadlock
     */
    public int lock(Runnable waitOp) {
        for( ; ; ) {
            int current = getVolatileValue();
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
    public void unlock(int next) {
        if(next == LOCK_VALUE || !casValue(LOCK_VALUE, next)) {
            throw new FrameworkException(ExceptionType.CONTEXT, Constants.UNREACHED);
        }
    }

    /**
     *   Transform current IntHolder to another state with exclusive lock guarded
     */
    public void transform(IntUnaryOperator operator, Runnable waitOp) {
        int r = lock(waitOp);
        try{
            r = operator.applyAsInt(r);
        }finally {
            unlock(r);
        }
    }

    /**
     *   Extract current IntHolder's value without state change, guarded by exclusive lock
     */
    public <T> T extract(IntFunction<T> func, Runnable waitOp) {
        int r = lock(waitOp);
        try{
            return func.apply(r);
        }finally {
            unlock(r);
        }
    }
}
