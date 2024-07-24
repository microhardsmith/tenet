import cc.zorcc.tenet.core.log.Logger;
import cc.zorcc.tenet.core.log.LoggerProvider;

module cc.zorcc.tenet.log {
    requires cc.zorcc.tenet.core;

    provides Logger with cc.zorcc.tenet.log.LoggerImpl;
    provides LoggerProvider with cc.zorcc.tenet.log.LoggerProviderImpl;
}