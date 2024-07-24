package cc.zorcc.tenet.core.toml;

import java.util.List;

public record TomlArrValue(
        List<TomlValue> values
) implements TomlValue {

}
