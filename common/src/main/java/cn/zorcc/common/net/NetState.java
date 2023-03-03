package cn.zorcc.common.net;

import java.lang.foreign.Arena;

public class NetState {
    private final Arena arena = Arena.openConfined();

    public Arena arena() {
        return arena;
    }
}
