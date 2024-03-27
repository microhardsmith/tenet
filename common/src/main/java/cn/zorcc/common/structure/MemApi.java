package cn.zorcc.common.structure;

import cn.zorcc.common.Constants;
import cn.zorcc.common.ExceptionType;
import cn.zorcc.common.exception.FrameworkException;
import cn.zorcc.common.util.NativeUtil;

import java.lang.foreign.FunctionDescriptor;
import java.lang.foreign.Linker;
import java.lang.foreign.MemorySegment;
import java.lang.foreign.ValueLayout;
import java.lang.invoke.MethodHandle;

public interface MemApi {
    /**
     *   Default system memApi, could be overwritten using LD_PRELOAD with custom memory allocators such as jemalloc
     */
    MemApi DEFAULT = new SystemMemApi();

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

    /**
     *   Operating system default memory allocator
     */
    record SystemMemApi() implements MemApi {
        private static final MethodHandle mallocHandle;
        private static final MethodHandle reallocHandle;
        private static final MethodHandle freeHandle;
        static {
            mallocHandle = NativeUtil.nativeMethodHandle("malloc", FunctionDescriptor.of(ValueLayout.ADDRESS, ValueLayout.JAVA_LONG), Linker.Option.critical(false));
            reallocHandle = NativeUtil.nativeMethodHandle("realloc", FunctionDescriptor.of(ValueLayout.ADDRESS, ValueLayout.ADDRESS, ValueLayout.JAVA_LONG), Linker.Option.critical(false));
            freeHandle = NativeUtil.nativeMethodHandle("free", FunctionDescriptor.ofVoid(ValueLayout.ADDRESS), Linker.Option.critical(false));
        }
        @Override
        public MemorySegment allocateMemory(long byteSize) {
            try {
                return (MemorySegment) mallocHandle.invokeExact(byteSize);
            } catch (Throwable e) {
                throw new FrameworkException(ExceptionType.NATIVE, Constants.UNREACHED);
            }
        }

        @Override
        public MemorySegment reallocateMemory(MemorySegment ptr, long newSize) {
            try{
                return (MemorySegment) reallocHandle.invokeExact(ptr, newSize);
            } catch (Throwable e) {
                throw new FrameworkException(ExceptionType.NATIVE, Constants.UNREACHED);
            }
        }

        @Override
        public void freeMemory(MemorySegment ptr) {
            try{
                freeHandle.invokeExact(ptr);
            } catch (Throwable e) {
                throw new FrameworkException(ExceptionType.NATIVE, Constants.UNREACHED);
            }
        }
    }
}
