package cn.zorcc.common.util;

import cn.zorcc.common.Constants;
import cn.zorcc.common.enums.ExceptionType;
import cn.zorcc.common.exception.FrameworkException;
import com.esotericsoftware.reflectasm.ConstructorAccess;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.io.IOException;
import java.io.InputStream;

/**
 * 配置加载类
 */
public class ConfigUtil {

    private ConfigUtil() {
        throw new UnsupportedOperationException();
    }

    /**
     * 加载项目配置json文件
     * @param fileName 配置文件名称
     * @param configClass 配置文件类
     * @return 配置类
     * @param <T> 配置类类型
     */
    public static <T> T loadJsonConfig(String fileName, Class<T> configClass) {
        if (!fileName.endsWith(Constants.JSON_SUFFIX)) {
            throw new FrameworkException(ExceptionType.CONFIG, "Config file must be .json");
        }
        if (!fileName.startsWith(Constants.SEPARATOR)) {
            fileName = Constants.SEPARATOR.concat(fileName);
        }
        ClassLoader contextClassLoader = Thread.currentThread().getContextClassLoader();
        try(InputStream jsonStream = contextClassLoader.getResourceAsStream(fileName)) {
            return jsonStream == null ? ConstructorAccess.get(configClass).newInstance() : new ObjectMapper().readValue(jsonStream, configClass);
        } catch (IOException e) {
            throw new FrameworkException(ExceptionType.CONFIG, "Can't resolve config file : " + fileName, e);
        }
    }
}
