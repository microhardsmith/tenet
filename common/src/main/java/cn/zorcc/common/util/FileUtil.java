package cn.zorcc.common.util;

import cn.zorcc.common.enums.ExceptionType;
import cn.zorcc.common.exception.FrameworkException;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;

public class FileUtil {
    private FileUtil() {
        throw new UnsupportedOperationException();
    }

    /**
     *  Copy target resource to the tmp dir
     * @param resourcePath relative path under resource folder
     * @param fileName the tmp fileName without extension
     * @param clazz class representing target resource folder
     * @return tmp file path
     */
    public static Path toTmp(String resourcePath, String fileName, Class<?> clazz) {
        String suffix = resourcePath.substring(resourcePath.lastIndexOf('.'));
        try(InputStream inputStream = (clazz == null ? NativeUtil.class : clazz).getResourceAsStream(resourcePath)) {
            if(inputStream == null) {
                throw new FrameworkException(ExceptionType.NATIVE, "ResourcePath is not valid");
            }else {
                final File tmp = File.createTempFile(fileName, suffix);
                Path path = tmp.toPath();
                Files.copy(inputStream, path, StandardCopyOption.REPLACE_EXISTING);
                if(tmp.exists()) {
                    // when JVM exit, the tmp file will be destroyed
                    tmp.deleteOnExit();
                }else {
                    throw new FrameworkException(ExceptionType.NATIVE, "File %s doesn't exist".formatted(tmp.getAbsolutePath()));
                }
                return path;
            }
        }catch (IOException e) {
            throw new FrameworkException(ExceptionType.NATIVE, "Unable to load library", e);
        }
    }
}
