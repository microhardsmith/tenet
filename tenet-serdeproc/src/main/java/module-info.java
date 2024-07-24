module cc.zorcc.tenet.serdeproc {
    requires java.compiler;
    requires cc.zorcc.tenet.serde;

    exports cc.zorcc.tenet.serdeproc;

    provides javax.annotation.processing.Processor with cc.zorcc.tenet.serdeproc.SerdeProcessor;
}