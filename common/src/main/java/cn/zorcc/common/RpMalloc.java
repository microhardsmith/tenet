package cn.zorcc.common;

import cn.zorcc.common.bindings.TenetBinding;
import cn.zorcc.common.exception.FrameworkException;
import cn.zorcc.common.structure.IntHolder;
import cn.zorcc.common.structure.MemApi;

import java.lang.foreign.MemorySegment;
import java.util.HashSet;
import java.util.Set;

/**
 *   RpMalloc helper class
 */
public final class RpMalloc {
    private static final IntHolder state = new IntHolder(Constants.INITIAL);
    private static final Set<Thread> registries = new HashSet<>();
    private static final MemApi INSTANCE = new MemApi() {
        @Override
        public MemorySegment allocateMemory(long byteSize) {
            return TenetBinding.rpMalloc(byteSize);
        }

        @Override
        public MemorySegment reallocateMemory(MemorySegment ptr, long newSize) {
            return TenetBinding.rpRealloc(ptr, newSize);
        }

        @Override
        public void freeMemory(MemorySegment ptr) {
            TenetBinding.rpFree(ptr);
        }
    };

    private RpMalloc() {
        throw new UnsupportedOperationException();
    }

    public static void initialize() {
        state.transform(current -> {
            if(current == Constants.INITIAL) {
                if(TenetBinding.rpInitialize() < 0) {
                    throw new FrameworkException(ExceptionType.NATIVE, "Failed to initialize RpMalloc allocator");
                }
                return Constants.RUNNING;
            }else {
                return current;
            }
        }, Thread::yield);
    }

    public static void release() {
        state.transform(current -> {
            if(current == Constants.RUNNING) {
                TenetBinding.rpFinalize();
                return Constants.STOPPED;
            }else {
                return current;
            }
        }, Thread::yield);
    }

    public static MemApi tInitialize() {
        Thread currentThread = Thread.currentThread();
        if(currentThread.isVirtual()) {
            throw new FrameworkException(ExceptionType.NATIVE, "Shouldn't be initializing rpMalloc in virtual threads");
        }
        return state.extract(current -> {
            if (current != Constants.RUNNING) {
                throw new FrameworkException(ExceptionType.NATIVE, Constants.UNREACHED);
            }
            if (registries.add(currentThread)) {
                TenetBinding.rpThreadInitialize();
            }
            return INSTANCE;
        }, Thread::onSpinWait);
    }

    public static void tRelease() {
        Thread currentThread = Thread.currentThread();
        if(currentThread.isVirtual()) {
            throw new FrameworkException(ExceptionType.NATIVE, "Shouldn't be initializing rpMalloc in virtual threads");
        }
        state.transform(current -> {
            if(current != Constants.RUNNING) {
                throw new FrameworkException(ExceptionType.NATIVE, Constants.UNREACHED);
            }
            if(registries.remove(currentThread)) {
                TenetBinding.rpThreadFinalize();
            }
            return current;
        }, Thread::onSpinWait);
    }
}
