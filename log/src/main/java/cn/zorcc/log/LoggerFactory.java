package cn.zorcc.log;

import org.slf4j.ILoggerFactory;

/**
 * logger工厂类
 */
public class LoggerFactory implements ILoggerFactory {

    @Override
    public org.slf4j.Logger getLogger(String name) {
        return new Logger(name);
    }
}
