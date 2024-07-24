package cc.zorcc.tenet.core;

import cc.zorcc.tenet.core.bindings.SysBinding;

import java.lang.foreign.MemorySegment;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 *   Mem is the abstraction of malloc(), free(), and realloc() using different memory allocator strategy
 */
public interface Mem {

    /**
     *   Default system memory allocator
     */
    Mem DEFAULT = new SysMem();

    /**
     *   Allocate a chunk of memory
     */
    MemorySegment allocateMemory(long byteSize);

    /**
     *   Reallocate a chunk of memory
     */
    MemorySegment reallocateMemory(MemorySegment ptr, long newSize);

    /**
     *   Free a chunk of memory
     */
    void freeMemory(MemorySegment ptr);

    final class SysMem implements Mem {
        private static final AtomicBoolean initialized = new AtomicBoolean(false);

        private SysMem() {
            if(!initialized.compareAndSet(false, true)) {
                throw new TenetException(ExceptionType.NATIVE, "SysMem shouldn't be created, use Mem.DEFAULT instead");
            }
        }

        @Override
        public MemorySegment allocateMemory(long byteSize) {
            return SysBinding.malloc(byteSize).reinterpret(byteSize);
        }

        @Override
        public MemorySegment reallocateMemory(MemorySegment ptr, long newSize) {
            return SysBinding.realloc(ptr, newSize).reinterpret(newSize);
        }

        @Override
        public void freeMemory(MemorySegment ptr) {
            SysBinding.free(ptr);
        }
    }
}
