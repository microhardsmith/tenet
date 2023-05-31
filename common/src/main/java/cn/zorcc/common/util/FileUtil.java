package cn.zorcc.common.util;

import cn.zorcc.common.Constants;
import cn.zorcc.common.enums.ExceptionType;
import cn.zorcc.common.exception.FrameworkException;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;

public class FileUtil {
    private FileUtil() {
        throw new UnsupportedOperationException();
    }

    /**
     *  Copy target resource to the tmp dir
     * @param resourcePath relative path under resource folder
     * @param clazz class representing target resource folder
     * @return tmp file absolute path
     */
    public static String toTmp(String resourcePath, Class<?> clazz) {
        int index = resourcePath.lastIndexOf('.');
        String prefix = resourcePath.substring(Math.max(resourcePath.lastIndexOf(Constants.SEPARATOR), 0), index);
        String suffix = resourcePath.substring(index);
        try(InputStream inputStream = (clazz == null ? FileUtil.class : clazz).getResourceAsStream(resourcePath)) {
            if(inputStream == null) {
                throw new FrameworkException(ExceptionType.NATIVE, "ResourcePath is not valid");
            }else {
                final File tmp = File.createTempFile(prefix, suffix);
                Files.copy(inputStream, tmp.toPath(), StandardCopyOption.REPLACE_EXISTING);
                if(tmp.exists()) {
                    // when JVM exit, the tmp file will be destroyed
                    tmp.deleteOnExit();
                }else {
                    throw new FrameworkException(ExceptionType.NATIVE, "File %s doesn't exist".formatted(tmp.getAbsolutePath()));
                }
                return tmp.getAbsolutePath();
            }
        }catch (IOException e) {
            throw new FrameworkException(ExceptionType.NATIVE, "Unable to load library", e);
        }
    }

    public static String toTmp(String resourcePath) {
        return toTmp(resourcePath, null);
    }
}
