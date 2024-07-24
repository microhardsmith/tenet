package cc.zorcc.tenet.serdetest;

import cc.zorcc.tenet.serde.Attr;
import cc.zorcc.tenet.serde.Serde;

@Serde
public final class Bean {
    @Attr({"json:str"})
    private Integer intValue;
    private String strValue;

    public String getStrValue() {
        return strValue;
    }

    public void setStrValue(String strValue) {
        this.strValue = strValue;
    }

    public Integer getIntValue() {
        return intValue;
    }

    public void setIntValue(Integer intValue) {
        this.intValue = intValue;
    }
}
