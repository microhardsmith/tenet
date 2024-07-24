package cc.zorcc.tenet.core;

import java.lang.invoke.MethodHandles;
import java.lang.invoke.VarHandle;
import java.util.function.IntFunction;
import java.util.function.IntUnaryOperator;

/**
 *   Int is a thin wrapper for a single int value, with different accessing behaviour provided, Int could be used as a spin lock
 */
@SuppressWarnings("unused")
public final class Int {
    private static final VarHandle handle;
    static {
        try {
            MethodHandles.Lookup lookup = MethodHandles.lookup();
            handle = lookup.findVarHandle(Int.class, "value", int.class).withInvokeExactBehavior();
        } catch (ReflectiveOperationException e) {
            throw new ExceptionInInitializerError();
        }
    }

    /**
     *   Declared as volatile for constructor usage
     */
    @SuppressWarnings("FieldMayBeFinal")
    private volatile int value;

    public Int(int initialValue) {
        this.value = initialValue;
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

    public int getAndBitwiseOr(int orValue) {
        return (int) handle.getAndBitwiseOr(this, orValue);
    }

    public int getAndBitwiseAnd(int andValue) {
        return (int) handle.getAndBitwiseAnd(this, andValue);
    }

    public int getAndBitwiseXor(int xorValue) {
        return (int) handle.getAndBitwiseXor(this, xorValue);
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

    private static final int LOCK_VALUE = Integer.MIN_VALUE;

    /**
     *   Acquiring an int lock, note that this function is not reentrant, acquiring a lock multiple times will cause deadlock
     *   Note that acquiring a lock when the value is set to LOCK_VALUE should be completely forbidden by developers, which will starve the thread without returning
     *   This method should only be used when you think there could be no locking at all for 99.99% the cases, otherwise, use ReentrantLock instead
     */
    public int lock() {
        for(int i = 0; i < Integer.MAX_VALUE; i++) {
            int current = getVolatileValue();
            if(current == LOCK_VALUE) {
                // other thread has acquired the lock, spin wait
                Thread.onSpinWait();
            }else if(casValue(current, LOCK_VALUE)) {
                //
                return current;
            }else {
                // cas failed because other thread has acquired the lock, spin wait
                Thread.onSpinWait();
            }
        }
        throw new TenetException(ExceptionType.MISUSE, "Unable to acquire the spin lock");
    }

    /**
     *   Pair with lock()
     */
    public void unlock(int next) {
        if(next == LOCK_VALUE || !casValue(LOCK_VALUE, next)) {
            throw new TenetException(ExceptionType.MISUSE, Constants.UNREACHED);
        }
    }

    /**
     *   Transform current IntHolder to another state with exclusive lock guarded
     */
    public void transform(IntUnaryOperator operator) {
        int r = lock();
        try{
            r = operator.applyAsInt(r);
        }finally {
            unlock(r);
        }
    }

    /**
     *   Extract current IntHolder's value without state change, guarded by exclusive lock
     */
    public <T> T extract(IntFunction<T> func) {
        int r = lock();
        try{
            return func.apply(r);
        }finally {
            unlock(r);
        }
    }
}
