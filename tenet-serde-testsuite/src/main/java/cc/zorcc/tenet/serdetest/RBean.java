package cc.zorcc.tenet.serdetest;

import cc.zorcc.tenet.serde.Attr;
import cc.zorcc.tenet.serde.Serde;

import java.util.List;
import java.util.Map;

/**
 *   Normal record bean test
 */
@Serde
public record RBean(
        @Attr({"json:str"}) Integer intValue,
        @Attr({"json:raw", "a1:a2", "a3:a4", "a5:a6"}) List<String> list,
        List<List<String>> doubleList,
        Map<Integer, String> map
) {
}
