package cn.zorcc.common;

import cn.zorcc.common.enums.ExceptionType;
import cn.zorcc.common.exception.FrameworkException;

import java.io.InputStream;
import java.lang.foreign.SegmentScope;
import java.lang.foreign.SymbolLookup;
import java.lang.invoke.MethodHandle;
import java.net.URL;

/**
 *  Helper class when need to reach C native methods
 */
public class NativeHelper {
    private static final String LIBRARY_PATH = "java.library.path";
    public static MethodHandle getNativeMethod() {
        // TODO
        return null;
    }

    /**
     * 从resource文件夹路径下加载动态链接库
     * @param resourcePath 动态链接库路径
     */
    public static SymbolLookup loadLibraryFromResource(String resourcePath) {
        URL resource = NativeHelper.class.getResource(resourcePath);
        if(resource == null) {
            throw new FrameworkException(ExceptionType.CONTEXT, "ResourcePath is not valid");
        }else {
            String path = resource.getPath();
            if(path.startsWith("/")) {
                path = path.substring(Constants.ONE);
            }
            return SymbolLookup.libraryLookup(path, SegmentScope.global());
        }
    }
}
