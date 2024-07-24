package cc.zorcc.tenet.core.log;

@FunctionalInterface
public interface LoggerProvider {

    /**
     *   Creating a new logger based on target class
     */
    Logger getLogger(Class<?> clazz);
}
