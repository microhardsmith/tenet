package cn.zorcc.common.util;

import cn.zorcc.common.Constants;
import cn.zorcc.common.Meta;
import cn.zorcc.common.ReadBuffer;
import cn.zorcc.common.enums.ExceptionType;
import cn.zorcc.common.exception.FrameworkException;
import cn.zorcc.common.json.JsonParser;

import java.io.IOException;
import java.io.InputStream;
import java.lang.foreign.MemorySegment;

/**
 * 配置加载类
 */
public class ConfigUtil {
    private ConfigUtil() {
        throw new UnsupportedOperationException();
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
        try(InputStream jsonStream = ConfigUtil.class.getClassLoader().getResourceAsStream(fileName)) {
            if(jsonStream == null) {
                return Meta.of(configClass).constructor().get();
            }else {
                return JsonParser.deserializeAsObject(new ReadBuffer(MemorySegment.ofArray(jsonStream.readAllBytes())), configClass);
            }
        } catch (IOException e) {
            throw new FrameworkException(ExceptionType.CONFIG, "Can't resolve config file : " + fileName, e);
        }
    }
}
