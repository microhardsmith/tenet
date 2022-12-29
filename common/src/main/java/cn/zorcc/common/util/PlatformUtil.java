package cn.zorcc.common.util;

/**
 * 操作系统相关工具类
 */
public class PlatformUtil {
    private static final String OS_NAME = System.getProperty("os.name").toLowerCase();
    private static final boolean LINUX = OS_NAME.contains("linux");
    private static final boolean WINDOWS = OS_NAME.contains("windows");
    private static final boolean MAC = OS_NAME.contains("mac") && OS_NAME.contains("os");
    private static final int CPU_CORES = Runtime.getRuntime().availableProcessors();

    private PlatformUtil() {
        throw new UnsupportedOperationException();
    }

    /**
     * 当前运行平台是否为linux操作系统
     */
    public static boolean isLinux() {
        return LINUX;
    }

    /**
     * 当前运行平台是否为windows操作系统
     */
    public static boolean isWindows() {
        return WINDOWS;
    }

    /**
     * 当前运行平台是否为mac操作系统
     */
    public static boolean isMac() {
        return MAC;
    }

    /**
     * 获取当前机器cpu核数
     */
    public static int getCpuCores() {
        return CPU_CORES;
    }
}
