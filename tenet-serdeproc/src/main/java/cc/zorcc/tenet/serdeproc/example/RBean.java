package cc.zorcc.tenet.serdeproc.example;

import cc.zorcc.tenet.serde.Attr;
import cc.zorcc.tenet.serde.Serde;

import java.util.List;
import java.util.Map;

@Serde
public record RBean(
        @Attr({"json:str"}) Integer intValue,
        @Attr({"json:raw"}) List<String> list,
        List<List<String>> doubleList,
        Map<Integer, String> map
) {
}
