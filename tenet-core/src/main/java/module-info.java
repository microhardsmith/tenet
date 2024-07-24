module cc.zorcc.tenet.core {
    requires cc.zorcc.tenet.serde;
    requires static cc.zorcc.tenet.serdeproc;
    requires jdk.incubator.vector;

    exports cc.zorcc.tenet.core.bindings;
    exports cc.zorcc.tenet.core.json;
    exports cc.zorcc.tenet.core.toml;
    exports cc.zorcc.tenet.core.log;
    exports cc.zorcc.tenet.core;

    uses cc.zorcc.tenet.core.log.LoggerProvider;
}