package cn.zorcc.common.net;

import cn.zorcc.common.util.ThreadUtil;

/**
 *  looper implementation under windows
 */
public class WinLooper implements Looper {
    private static final String NAME = "winLooper";
    private final Thread loopThread;
    public WinLooper() {
        this.loopThread = ThreadUtil.platform(NAME, () -> {

        });
    }
}
