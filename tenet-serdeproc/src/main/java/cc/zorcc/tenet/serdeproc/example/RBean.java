package cc.zorcc.tenet.serdeproc.example;

import cc.zorcc.tenet.serde.Attr;
import cc.zorcc.tenet.serde.Serde;

@Serde
public record RBean(
        @Attr({"json:str"}) Integer intValue
) {
}
