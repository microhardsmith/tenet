package cc.zorcc.tenet.serdeproc.example;

import cc.zorcc.tenet.serde.Attr;
import cc.zorcc.tenet.serde.Serde;

@Serde
public final class Bean {
    @Attr({"json:str"})
    private Integer intValue;

    public Integer getIntValue() {
        return intValue;
    }

    public void setIntValue(Integer intValue) {
        this.intValue = intValue;
    }
}
