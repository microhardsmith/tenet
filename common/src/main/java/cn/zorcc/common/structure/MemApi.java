package cn.zorcc.common.structure;

import cn.zorcc.common.bindings.SystemBinding;

import java.lang.foreign.MemorySegment;

public interface MemApi {
    /**
     *   Default system memApi, could be overwritten using LD_PRELOAD with custom memory allocators such as jemalloc, tcmalloc, or mimalloc
     */
    MemApi DEFAULT = new MemApi() {
        @Override
        public MemorySegment allocateMemory(long byteSize) {
            return SystemBinding.malloc(byteSize);
        }

        @Override
        public MemorySegment reallocateMemory(MemorySegment ptr, long newSize) {
            return SystemBinding.realloc(ptr, newSize);
        }

        @Override
        public void freeMemory(MemorySegment ptr) {
            SystemBinding.free(ptr);
        }
    };

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

}
