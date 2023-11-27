package cn.zorcc.common.util;

import cn.zorcc.common.Constants;
import cn.zorcc.common.ExceptionType;
import cn.zorcc.common.OsType;
import cn.zorcc.common.exception.FrameworkException;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.lang.foreign.FunctionDescriptor;
import java.lang.foreign.MemorySegment;
import java.lang.foreign.ValueLayout;
import java.lang.invoke.MethodHandle;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;

public final class FileUtil {
    private FileUtil() {
        throw new UnsupportedOperationException();
    }
    private static final MethodHandle setvbufMethodHandle = NativeUtil.nativeMethodHandle("setvbuf", FunctionDescriptor.of(ValueLayout.JAVA_INT, ValueLayout.ADDRESS, ValueLayout.ADDRESS, ValueLayout.JAVA_INT, ValueLayout.JAVA_LONG));
    private static final MethodHandle fopenMethodHandle = NativeUtil.nativeMethodHandle("fopen", FunctionDescriptor.of(ValueLayout.ADDRESS, ValueLayout.ADDRESS, ValueLayout.ADDRESS));
    private static final MethodHandle fwriteMethodHandle = NativeUtil.nativeMethodHandle("fwrite", FunctionDescriptor.of(ValueLayout.JAVA_LONG, ValueLayout.ADDRESS, ValueLayout.JAVA_LONG, ValueLayout.JAVA_LONG, ValueLayout.ADDRESS));
    private static final MethodHandle fflushMethodHandle = NativeUtil.nativeMethodHandle("fflush", FunctionDescriptor.of(ValueLayout.JAVA_INT, ValueLayout.ADDRESS));
    private static final MethodHandle fcloseMethodHandle = NativeUtil.nativeMethodHandle("fclose", FunctionDescriptor.of(ValueLayout.JAVA_INT, ValueLayout.ADDRESS));

    public static int setvbuf(MemorySegment stream, MemorySegment buffer, int mode, long size) {
        try{
            return (int) setvbufMethodHandle.invokeExact(stream, buffer, mode, size);
        }catch (Throwable throwable) {
            throw new FrameworkException(ExceptionType.NATIVE, Constants.UNREACHED, throwable);
        }
    }

    public static MemorySegment fopen(MemorySegment path, MemorySegment mode) {
        try{
            return (MemorySegment) fopenMethodHandle.invokeExact(path, mode);
        }catch (Throwable throwable) {
            throw new FrameworkException(ExceptionType.NATIVE, Constants.UNREACHED, throwable);
        }
    }

    public static void fwrite(MemorySegment buffer, MemorySegment stream) {
        try{
            long r = (long) fwriteMethodHandle.invokeExact(buffer, ValueLayout.JAVA_BYTE.byteSize(), buffer.byteSize(), stream);
            if(r < 0) {
                throw new FrameworkException(ExceptionType.NATIVE, "Unable to call fwrite()");
            }
        }catch (Throwable throwable) {
            throw new FrameworkException(ExceptionType.NATIVE, Constants.UNREACHED, throwable);
        }
    }

    public static void fflush(MemorySegment stream) {
        try{
            int r = (int) fflushMethodHandle.invokeExact(stream);
            if(r < 0) {
                throw new FrameworkException(ExceptionType.NATIVE, "Unable to call fflush()");
            }
        }catch (Throwable throwable) {
            throw new FrameworkException(ExceptionType.NATIVE, Constants.UNREACHED, throwable);
        }
    }

    public static void fclose(MemorySegment stream) {
        try{
            int r = (int) fcloseMethodHandle.invokeExact(stream);
            if(r < 0) {
                throw new FrameworkException(ExceptionType.NATIVE, "Unable to call fclose()");
            }
        }catch (Throwable throwable) {
            throw new FrameworkException(ExceptionType.NATIVE, Constants.UNREACHED, throwable);
        }
    }

    public static String normalizePath(String path) {
        if(NativeUtil.ostype() == OsType.Windows) {
            return path.replace("\\", "/");
        }else {
            return path;
        }
    }

    /**
     *  Copy target resource to the tmp dir
     * @param resourcePath relative path under resource folder
     * @param clazz class representing target resource folder
     * @return tmp file absolute path
     */
    public static String toTmp(String resourcePath, Class<?> clazz) {
        int index = resourcePath.lastIndexOf('.');
        String prefix = resourcePath.substring(Math.max(resourcePath.lastIndexOf(Constants.SEPARATOR), 0), index);
        String suffix = resourcePath.substring(index);
        try(InputStream inputStream = (clazz == null ? FileUtil.class : clazz).getResourceAsStream(resourcePath)) {
            if(inputStream == null) {
                throw new FrameworkException(ExceptionType.NATIVE, "ResourcePath is not valid");
            }else {
                final File tmp = File.createTempFile(prefix, suffix);
                if(tmp.exists()) {
                    Files.copy(inputStream, tmp.toPath(), StandardCopyOption.REPLACE_EXISTING);
                    tmp.deleteOnExit();
                }else {
                    throw new FrameworkException(ExceptionType.NATIVE, "File %s doesn't exist".formatted(tmp.getAbsolutePath()));
                }
                return tmp.getAbsolutePath();
            }
        }catch (IOException e) {
            throw new FrameworkException(ExceptionType.NATIVE, "Unable to load library", e);
        }
    }

    public static String toTmp(String resourcePath) {
        return toTmp(resourcePath, null);
    }
}
