package cc.zorcc.tenet.core;

import cc.zorcc.tenet.core.bindings.TenetBinding;

import java.lang.foreign.MemorySegment;
import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

public final class RpMem {
    /**
     *   Global instance
     */
    private static final Mem INSTANCE = new Mem() {
        @Override
        public MemorySegment allocateMemory(long byteSize) {
            return TenetBinding.rpMalloc(byteSize).reinterpret(byteSize);
        }

        @Override
        public MemorySegment reallocateMemory(MemorySegment ptr, long newSize) {
            return TenetBinding.rpRealloc(ptr, newSize).reinterpret(newSize);
        }

        @Override
        public void freeMemory(MemorySegment ptr) {
            TenetBinding.rpFree(ptr);
        }
    };

    /**
     *   RpMalloc lock
     */
    private static final Lock lock = new ReentrantLock();

    /**
     *   RpMalloc thread registries
     */
    private static final Set<Thread> registries = new HashSet<>();

    /**
     *   RpMem shouldn't be directly initialized
     */
    private RpMem() {
        throw new UnsupportedOperationException();
    }

    /**
     *   Obtaining rpmalloc instance, this function should only be called by platform thread
     */
    public static Mem acquire() {
        Thread currentThread = Thread.currentThread();
        if(currentThread.isVirtual()) {
            throw new TenetException(ExceptionType.NATIVE, "Shouldn't be acquiring rpMalloc in virtual thread");
        }
        lock.lock();
        try {
            if(registries.isEmpty()) {
                if(TenetBinding.rpInitialize() < 0) {
                    throw new TenetException(ExceptionType.NATIVE, "Failed to initialize RpMalloc allocator");
                }
            }
            if(registries.add(currentThread)) {
                TenetBinding.rpThreadInitialize();
            }
            return INSTANCE;
        } finally {
            lock.unlock();
        }
    }

    /**
     *   Pair with acquire(), release the resources for current thread
     */
    public static void release() {
        Thread currentThread = Thread.currentThread();
        if(currentThread.isVirtual()) {
            throw new TenetException(ExceptionType.NATIVE, "Shouldn't be releasing rpMalloc in virtual thread");
        }
        lock.lock();
        try {
            if(registries.remove(currentThread)) {
                TenetBinding.rpThreadFinalize();
            }
            if(registries.isEmpty()) {
                TenetBinding.rpFinalize();
            }
        } finally {
            lock.unlock();
        }
    }
}
