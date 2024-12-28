package cc.zorcc.tenet.serdetest;

import cc.zorcc.tenet.serde.Attr;
import cc.zorcc.tenet.serde.Serde;

import java.util.List;
import java.util.Map;

/**
 *   Normal enum bean test
 */
@Serde
public enum EBean {
    Test1(123, List.of("hello"), List.of(List.of("test1")), Map.of(123, "abc")),
    Test2(456, List.of("world"), List.of(List.of("test2")), Map.of(456, "bcd"));

    @Attr({"json:str"})
    private final Integer intValue;
    @Attr({"json:raw", "a1:a2", "a3:a4", "a5:a6"})
    private final List<String> list;
    private final List<List<String>> doubleList;
    private final Map<Integer, String> map;

    EBean(Integer intValue, List<String> list, List<List<String>> doubleList, Map<Integer, String> map) {
        this.intValue = intValue;
        this.list = list;
        this.doubleList = doubleList;
        this.map = map;
    }

    public Integer getIntValue() {
        return intValue;
    }

    public List<String> getList() {
        return list;
    }

    public List<List<String>> getDoubleList() {
        return doubleList;
    }

    public Map<Integer, String> getMap() {
        return map;
    }
}
