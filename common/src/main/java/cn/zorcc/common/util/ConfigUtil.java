package cn.zorcc.common.util;

import cn.zorcc.common.Constants;
import cn.zorcc.common.ExceptionType;
import cn.zorcc.common.Meta;
import cn.zorcc.common.exception.FrameworkException;
import cn.zorcc.common.json.JsonParser;
import cn.zorcc.common.structure.ReadBuffer;

import java.io.IOException;
import java.io.InputStream;
import java.lang.foreign.MemorySegment;

public final class ConfigUtil {
    private ConfigUtil() {
        throw new UnsupportedOperationException();
    }

    /**
     *   Load json configuration file from resources folder, create a default one if not found
     */
    public static <T> T loadJsonConfig(String fileName, Class<T> configClass) {
        if (!fileName.endsWith(Constants.JSON_SUFFIX)) {
            throw new FrameworkException(ExceptionType.CONFIG, "Config file must be .json");
        }
        try(InputStream jsonStream = ConfigUtil.class.getResourceAsStream(fileName)) {
            if(jsonStream == null) {
                return Meta.of(configClass).constructor().get();
            }else {
                return JsonParser.readObject(new ReadBuffer(MemorySegment.ofArray(jsonStream.readAllBytes())), configClass);
            }
        } catch (IOException e) {
            throw new FrameworkException(ExceptionType.CONFIG, "Can't resolve config file : " + fileName, e);
        }
    }
}
