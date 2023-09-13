package cn.zorcc.common.util;

import cn.zorcc.common.Constants;
import cn.zorcc.common.enums.ExceptionType;
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
    private static final MethodHandle fopenMethodHandle = NativeUtil.nativeMethodHandle("fopen", FunctionDescriptor.of(ValueLayout.ADDRESS, ValueLayout.ADDRESS, ValueLayout.ADDRESS));
    private static final MethodHandle fwriteMethodHandle = NativeUtil.nativeMethodHandle("fwrite", FunctionDescriptor.of(ValueLayout.JAVA_LONG, ValueLayout.ADDRESS, ValueLayout.JAVA_LONG, ValueLayout.JAVA_LONG, ValueLayout.ADDRESS));
    private static final MethodHandle fflushMethodHandle = NativeUtil.nativeMethodHandle("fflush", FunctionDescriptor.of(ValueLayout.JAVA_INT, ValueLayout.ADDRESS));
    private static final MethodHandle fcloseMethodHandle = NativeUtil.nativeMethodHandle("fclose", FunctionDescriptor.of(ValueLayout.JAVA_INT, ValueLayout.ADDRESS));

    public static MemorySegment fopen(MemorySegment path, MemorySegment mode) {
        try{
            return (MemorySegment) fopenMethodHandle.invokeExact(path, mode);
        }catch (Throwable throwable) {
            throw new FrameworkException(ExceptionType.LOG, Constants.UNREACHED, throwable);
        }
    }

    public static void fwrite(MemorySegment buffer, MemorySegment stream) {
        try{
            long r = (long) fwriteMethodHandle.invokeExact(buffer, ValueLayout.JAVA_BYTE.byteSize(), buffer.byteSize(), stream);
            if(r < Constants.ZERO) {
                throw new FrameworkException(ExceptionType.LOG, "Unable to call fputs()");
            }
        }catch (Throwable throwable) {
            throw new FrameworkException(ExceptionType.LOG, Constants.UNREACHED, throwable);
        }
    }

    public static void fflush(MemorySegment stream) {
        try{
            int r = (int) fflushMethodHandle.invokeExact(stream);
            if(r < Constants.ZERO) {
                throw new FrameworkException(ExceptionType.LOG, "Unable to call fflush()");
            }
        }catch (Throwable throwable) {
            throw new FrameworkException(ExceptionType.LOG, Constants.UNREACHED, throwable);
        }
    }

    public static void fclose(MemorySegment stream) {
        try{
            int r = (int) fcloseMethodHandle.invokeExact(stream);
            if(r < Constants.ZERO) {
                throw new FrameworkException(ExceptionType.LOG, "Unable to call fclose()");
            }
        }catch (Throwable throwable) {
            throw new FrameworkException(ExceptionType.LOG, Constants.UNREACHED, throwable);
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
        String prefix = resourcePath.substring(Math.max(resourcePath.lastIndexOf(Constants.SEPARATOR), Constants.ZERO), index);
        String suffix = resourcePath.substring(index);
        try(InputStream inputStream = (clazz == null ? FileUtil.class : clazz).getResourceAsStream(resourcePath)) {
            if(inputStream == null) {
                throw new FrameworkException(ExceptionType.NATIVE, "ResourcePath is not valid");
            }else {
                final File tmp = File.createTempFile(prefix, suffix);
                Files.copy(inputStream, tmp.toPath(), StandardCopyOption.REPLACE_EXISTING);
                if(tmp.exists()) {
                    // when JVM exit, the tmp file will be destroyed
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
