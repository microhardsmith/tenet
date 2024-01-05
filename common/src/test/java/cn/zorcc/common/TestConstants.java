package cn.zorcc.common;

import cn.zorcc.common.exception.FrameworkException;
import cn.zorcc.common.network.IpType;
import cn.zorcc.common.network.Loc;
import cn.zorcc.common.util.NativeUtil;

public final class TestConstants {
    public static final int PORT = port();
    public static final Loc SERVER_IPV4_LOC = new Loc(IpType.IPV4, PORT);
    public static final Loc SERVER_IPV6_LOC = new Loc(IpType.IPV6, PORT);
    public static final Loc CLIENT_IPV4_LOC = new Loc(IpType.IPV4, "127.0.0.1", PORT);
    public static final Loc CLIENT_IPV6_LOC = new Loc(IpType.IPV6, "::1", PORT);
    public static final Loc HTTP_LOC = new Loc(IpType.IPV4, 80);
    public static final Loc HTTPS_LOC = new Loc(IpType.IPV6, 443);
    public static final String SELF_PUBLIC_KEY_FILE = selfPublicKeyFile();
    public static final String SELF_PRIVATE_KEY_FILE = selfPrivateKeyFile();
    public static final String SERVER_PUBLIC_KEY_FILE = serverPublicKeyFile();
    public static final String SERVER_PRIVATE_KEY_FILE = serverPrivateKeyFile();

    /**
     *   Rerunning program in the same operating system would be fine, but wsl would share the same port between Windows and Linux, so we might want to differ them
     */
    private static int port() {
        OsType ostype = NativeUtil.ostype();
        if(ostype == OsType.Windows) {
            return 8001;
        }else if(ostype == OsType.MacOS) {
            return 8002;
        }else if(ostype == OsType.Linux) {
            return 8003;
        }else {
            throw new FrameworkException(ExceptionType.NETWORK, Constants.UNREACHED);
        }
    }

    private static String selfPublicKeyFile() {
        OsType ostype = NativeUtil.ostype();
        if(ostype == OsType.Windows) {
            return "C:/workspace/ca/self.crt";
        }else if(ostype == OsType.MacOS) {
            return "/Users/liuxichen/workspace/ca/self.crt";
        }else if(ostype == OsType.Linux) {
            return "/home/liuxichen/workspace/ca/self.crt";
        }else {
            throw new FrameworkException(ExceptionType.NETWORK, Constants.UNREACHED);
        }
    }

    private static String selfPrivateKeyFile() {
        OsType ostype = NativeUtil.ostype();
        if(ostype == OsType.Windows) {
            return "C:/workspace/ca/self.key";
        }else if(ostype == OsType.MacOS) {
            return "/Users/liuxichen/workspace/ca/self.key";
        }else if(ostype == OsType.Linux) {
            return "/home/liuxichen/workspace/ca/self.key";
        }else {
            throw new FrameworkException(ExceptionType.NETWORK, Constants.UNREACHED);
        }
    }

    private static String serverPublicKeyFile() {
        OsType ostype = NativeUtil.ostype();
        if(ostype == OsType.Windows) {
            return "C:/workspace/ca/server.crt";
        }else if(ostype == OsType.MacOS) {
            return "/Users/liuxichen/workspace/ca/server.crt";
        }else if(ostype == OsType.Linux) {
            return "/home/liuxichen/workspace/ca/server.crt";
        }else {
            throw new FrameworkException(ExceptionType.NETWORK, Constants.UNREACHED);
        }
    }
    private static String serverPrivateKeyFile() {
        OsType ostype = NativeUtil.ostype();
        if(ostype == OsType.Windows) {
            return "C:/workspace/ca/server.key";
        }else if(ostype == OsType.MacOS) {
            return "/Users/liuxichen/workspace/ca/server.key";
        }else if(ostype == OsType.Linux) {
            return "/home/liuxichen/workspace/ca/server.key";
        }else {
            throw new FrameworkException(ExceptionType.NETWORK, Constants.UNREACHED);
        }
    }
}
