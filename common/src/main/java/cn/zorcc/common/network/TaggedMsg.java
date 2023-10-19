package cn.zorcc.common.network;

import cn.zorcc.common.Carrier;

public class TaggedMsg extends Carrier {
    private final int tag;

    public TaggedMsg(int tag) {
        this.tag = tag;
    }

    public int getTag() {
        return tag;
    }
}
