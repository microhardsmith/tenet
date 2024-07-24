package cc.zorcc.tenet.core.toml;

import cc.zorcc.tenet.core.ReadBuffer;

public final class Toml {
    /**
     *   Toml shouldn't be directly initialized
     */
    private Toml() {
        throw new UnsupportedOperationException();
    }

    /**
     *   Creating a tomlTable based on given readBuffer content
     */
    public TomlTable createTomlTable(ReadBuffer readBuffer) {
        // TODO
        TomlParser parser = new TomlParser(readBuffer);
        return null;
    }
}
