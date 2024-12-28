module cc.zorcc.tenet.serdeproc {
    requires java.compiler;
    requires cc.zorcc.tenet.serde;
    requires org.jspecify;
    requires java.management;

    exports cc.zorcc.tenet.serdeproc;

    provides javax.annotation.processing.Processor with cc.zorcc.tenet.serdeproc.SerdeProcessor, cc.zorcc.tenet.serdeproc.TransformProcessor;
}