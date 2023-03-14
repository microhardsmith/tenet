package cn.zorcc.common.net;

import cn.zorcc.common.enums.ExceptionType;
import cn.zorcc.common.exception.FrameworkException;
import cn.zorcc.common.net.linux.LinuxLooper;
import cn.zorcc.common.net.macos.MacLooper;
import cn.zorcc.common.net.win.WinLooper;
import cn.zorcc.common.pojo.Loc;
import cn.zorcc.common.util.NativeUtil;

public class Net {
    private final Looper looper;

    public Net(NetConfig netConfig) {
        this.looper = createLooper(netConfig);
    }

    private Looper createLooper(NetConfig netConfig) {
        if(NativeUtil.isLinux()) {
            return new LinuxLooper(netConfig);
        }else if(NativeUtil.isWindows()) {
            return new WinLooper(netConfig);
        }else if(NativeUtil.isMacos()){
            return new MacLooper(netConfig);
        }else {
            throw new FrameworkException(ExceptionType.NET, "Unsupported operating system");
        }
    }
}
