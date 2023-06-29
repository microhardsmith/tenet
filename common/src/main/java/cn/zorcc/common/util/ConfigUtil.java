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
     *   Check ipv4 address string format
     */
    public static boolean checkIp(String ip) {
        if (ip.isBlank()) {
            return false;
        }
        String[] strings = ip.split("\\.");
        for (String s : strings) {
            try{
                int value = Integer.parseInt(s);
                if(value < 0 || value > 255) {
                    return false;
                }
            }catch (NumberFormatException e) {
                return false;
            }
        }
        return true;
    }

    /**
     *   Check network port range
     */
    public static boolean checkPort(int port) {
        return port >= 0 && port <= 65535;
    }

    /**
     * 加载项目配置json文件,从调用者线程检索项目resource下json文件
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
