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
            handle = lookup.findVarHandle(IntHolder.class, "value", int.class);
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
        return value;
    }

    public void setValue(int newValue) {
        this.value = newValue;
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

    private static final int MASK = 1 << 31;

    /**
     *   Acquiring an int lock, note that this function is not reentrant, acquiring a lock multiple times will cause deadlock
     */
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

    /**
     *   Transform current IntHolder to another state with exclusive lock guarded
     */
    public void transform(IntUnaryOperator operator, Runnable waitOp) {
        int current = lock(waitOp);
        int next = current;
        try{
            next = operator.applyAsInt(current);
        }finally {
            unlock(current, next);
        }
    }

    /**
     *   Extract current IntHolder's value without state change, guarded by exclusive lock
     */
    public <T> T extract(IntFunction<T> func, Runnable waitOp) {
        int current = lock(waitOp);
        try{
            return func.apply(current);
        }finally {
            unlock(current, current);
        }
    }
}
