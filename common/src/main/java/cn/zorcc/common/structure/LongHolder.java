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
            handle = lookup.findVarHandle(LongHolder.class, "value", long.class);
        } catch (ReflectiveOperationException e) {
            throw new FrameworkException(ExceptionType.CONTEXT, Constants.UNREACHED);
        }
    }

    private volatile long value;

    public LongHolder(long initialValue) {
        this.value = initialValue;
    }

    public void increment() {
        setValue(getValue() + 1L);
    }

    public void decrement() {
        setValue(getValue() - 1L);
    }

    public long getValue() {
        return (long) handle.get(this);
    }

    public void setValue(long newValue) {
        handle.set(this, newValue);
    }

    public long getVolatileValue() {
        return value;
    }

    public void setVolatileValue(long newValue) {
        this.value = newValue;
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

    /**
     *   Transform current LongHolder to another state without lock acquired
     */
    public void transform(LongUnaryOperator operator) {
        setValue(operator.applyAsLong(getValue()));
    }

    /**
     *   Extract current LongHolder's value without state change or lock
     */
    public <T> T extract(LongFunction<T> func) {
        return func.apply(getValue());
    }

}
