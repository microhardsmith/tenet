package cc.zorcc.tenet.serdetest;

import cc.zorcc.tenet.serde.Attr;
import cc.zorcc.tenet.serde.Serde;

@Serde
public enum EBean {
    Test1(123, "hello"),
    Test2(456, "world");

    @Attr({"json:str"})
    private final Integer intValue;
    private final String strValue;

    EBean(Integer intValue, String strValue) {
        this.intValue = intValue;
        this.strValue = strValue;
    }

    public Integer getIntValue() {
        return intValue;
    }

    public String getStrValue() {
        return strValue;
    }
}
