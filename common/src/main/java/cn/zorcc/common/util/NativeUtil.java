package cn.zorcc.common.util;

import cn.zorcc.common.enums.ExceptionType;
import cn.zorcc.common.exception.FrameworkException;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.lang.foreign.*;
import java.lang.invoke.MethodHandle;
import java.lang.invoke.VarHandle;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 *  Helper class when need to reach C native methods
 */
public class NativeUtil {
    private static final String OS_NAME = System.getProperty("os.name").toLowerCase();
    private static final boolean LINUX = OS_NAME.contains("linux");
    private static final boolean WINDOWS = OS_NAME.contains("windows");
    private static final boolean MAC = OS_NAME.contains("mac") && OS_NAME.contains("os");
    private static final int CPU_CORES = Runtime.getRuntime().availableProcessors();
    private static final Linker linker = Linker.nativeLinker();
    /**
     *  TODO 需要加一个shutdown hook 参考https://xie.infoq.cn/article/96202307b31fe425936f899ef
     */
    private static final Arena globalArena = Arena.openShared();
    /**
     *   临时库拷贝文件名
     */
    private static final String tmpLibName = "tenet-lib";
    /**
     *   动态链接库缓存,避免重复加载
     */
    private static final Map<String, SymbolLookup> cache = new ConcurrentHashMap<>();

    private NativeUtil() {
        throw new UnsupportedOperationException();
    }

    /**
     *   获取当前系统CPU核心数
     */
    public static int getCpuCores() {
        return CPU_CORES;
    }

    /**
     *   获取当前操作系统对应的动态链接库路径字符串
     */
    public static String commonLib() {
        if(LINUX) {
            return "/lib/lib_linux.so";
        }else if(WINDOWS) {
            return "/lib/lib_win.dll";
        }else if(MAC) {
            return "/lib/lib_macos.dylib";
        }else {
            throw new FrameworkException(ExceptionType.NATIVE, "Unsupported operating system : %s".formatted(OS_NAME));
        }
    }

    /**
     * 从C动态库中获取指定函数的MethodHandle
     * @param lookup 函数库地址
     * @param methodName 函数名称
     * @param functionDescriptor 函数参数描述
     * @return 对应MethodHandle
     */
    public static MethodHandle methodHandle(SymbolLookup lookup, String methodName, FunctionDescriptor functionDescriptor) {
        MemorySegment methodPointer = lookup.find(methodName)
                .orElseThrow(() -> new FrameworkException(ExceptionType.NATIVE, "Unable to load target native method : %s", methodName));
        return linker.downcallHandle(methodPointer, functionDescriptor);
    }

    /**
     *  从当前的common模块下resource中加载动态链接库
     */
    public static SymbolLookup loadLibraryFromResource(String resourcePath) {
        return loadLibraryFromResource(resourcePath, null);
    }

    /**
     * 从指定Class下的resource文件夹路径加载动态链接库 必须在platform thread下进行操作,因为拷贝文件可能阻塞
     * @param resourcePath 动态链接库路径
     * @param clazz 需要加载资源的检索类,如果指定为null则从common项目下加载
     * @return 指定库对应SymbolLookup
     */
    public static SymbolLookup loadLibraryFromResource(String resourcePath, Class<?> clazz) {
        return cache.computeIfAbsent(resourcePath, k -> {
            String suffix = k.substring(k.lastIndexOf('.'));
            try(InputStream inputStream = (clazz == null ? NativeUtil.class : clazz).getResourceAsStream(k)) {
                if(inputStream == null) {
                    throw new FrameworkException(ExceptionType.NATIVE, "ResourcePath is not valid");
                }else {
                    final File tmp = File.createTempFile(tmpLibName, suffix);
                    Path path = tmp.toPath();
                    Files.copy(inputStream, path, StandardCopyOption.REPLACE_EXISTING);
                    if(tmp.exists()) {
                        // when JVM exit, the tmp file will be destroyed
                        tmp.deleteOnExit();
                    }else {
                        throw new FrameworkException(ExceptionType.NATIVE, "File %s doesn't exist".formatted(tmp.getAbsolutePath()));
                    }
                    return SymbolLookup.libraryLookup(path, SegmentScope.global());
                }
            }catch (IOException e) {
                throw new FrameworkException(ExceptionType.NATIVE, "Unable to load library", e);
            }
        });
    }

    /**
     *  从操作系统已加载的动态链接库中获取函数索引
     */
    public static MethodHandle getNativeMethodHandle(String methodName, FunctionDescriptor functionDescriptor) {
        return linker.downcallHandle(linker.defaultLookup()
                .find(methodName)
                .orElseThrow(() -> new FrameworkException(ExceptionType.NATIVE, "Unable to locate [%s] native method".formatted(methodName))),
                functionDescriptor);
    }

    /**
     *  检查MemorySegment是否为空指针
     */
    public static boolean checkNullPointer(MemorySegment memorySegment) {
        return memorySegment == null || memorySegment.address() == 0L;
    }

    /**
     * 分配基本类型的对象,返回其指针
     * @param arena 内存作用域
     * @param memoryLayout 内存布局
     * @param value 对象值
     * @return 指向该对象的指针
     */
    public static MemorySegment allocate(Arena arena, MemoryLayout memoryLayout, Object value) {
        if(memoryLayout == null) {
            throw new FrameworkException(ExceptionType.NET, "Empty memoryLayout");
        }
        VarHandle varHandle = memoryLayout.varHandle();
        MemorySegment memorySegment = arena.allocate(memoryLayout);
        if(memoryLayout.equals(ValueLayout.JAVA_INT) && value instanceof Integer i) {
            varHandle.set(memorySegment, i);
        } else if(memoryLayout.equals(ValueLayout.JAVA_BYTE) && value instanceof Byte b) {
            varHandle.set(memorySegment, b);
        }else if(memoryLayout.equals(ValueLayout.JAVA_CHAR) && value instanceof Character c) {
            varHandle.set(memorySegment, c);
        }else if (memoryLayout.equals(ValueLayout.JAVA_BOOLEAN) && value instanceof Boolean b) {
            varHandle.set(memorySegment, b);
        } else if(memoryLayout.equals(ValueLayout.JAVA_LONG) && value instanceof Long l) {
            varHandle.set(memorySegment, l);
        } else if(memoryLayout.equals(ValueLayout.JAVA_FLOAT) && value instanceof Float f) {
            varHandle.set(memorySegment, f);
        }else if(memoryLayout.equals(ValueLayout.JAVA_DOUBLE) && value instanceof Double d) {
            varHandle.set(memorySegment, d);
        }else if(memoryLayout.equals(ValueLayout.JAVA_SHORT) && value instanceof Short s) {
            varHandle.set(memorySegment, s);
        }
        return memorySegment;
    }

    /**
     *   返回与jvm生命周期相同的全局堆内存
     */
    public static MemorySegment globalHeapSegment(String str) {
        return MemorySegment.ofArray(str.getBytes(StandardCharsets.UTF_8));
    }

    /**
     *  返回与jvm生命周期相同的全局直接内存
     */
    public static MemorySegment globalNativeSegment(String str) {
        return globalArena.allocateArray(ValueLayout.JAVA_BYTE, str.getBytes(StandardCharsets.UTF_8));
    }
}
