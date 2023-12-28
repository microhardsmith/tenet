package cn.zorcc.common;

import cn.zorcc.common.exception.FrameworkException;
import cn.zorcc.common.network.IpType;
import cn.zorcc.common.network.Loc;
import cn.zorcc.common.util.NativeUtil;

public class TestConstants {
    public static final int PORT = 8002;
    public static final Loc SERVER_IPV4_LOC = new Loc(IpType.IPV4, PORT);
    public static final Loc SERVER_IPV6_LOC = new Loc(IpType.IPV6, PORT);
    public static final Loc CLIENT_IPV4_LOC = new Loc(IpType.IPV4, "127.0.0.1", PORT);
    public static final Loc CLIENT_IPV6_LOC = new Loc(IpType.IPV6, "::1", PORT);
    public static final Loc HTTP_LOC = new Loc(IpType.IPV4, "0.0.0.0", 80);
    public static final Loc HTTPS_LOC = new Loc(IpType.IPV6, "0.0.0.0", 443);
    public static final String SELF_PUBLIC_KEY_FILE = selfPublicKeyFile();
    public static final String SELF_PRIVATE_KEY_FILE = selfPrivateKeyFile();
    public static final String SERVER_PUBLIC_KEY_FILE = serverPublicKeyFile();
    public static final String SERVER_PRIVATE_KEY_FILE = serverPrivateKeyFile();

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
