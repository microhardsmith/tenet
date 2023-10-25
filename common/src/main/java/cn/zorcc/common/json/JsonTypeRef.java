package cn.zorcc.common.json;

import cn.zorcc.common.Constants;
import cn.zorcc.common.enums.ExceptionType;
import cn.zorcc.common.exception.FrameworkException;
import cn.zorcc.common.exception.JsonParseException;

import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;

/**
 *   Helper class to get generic type from parameters
 */
public abstract class JsonTypeRef<T> {
    private final Type type;

    protected JsonTypeRef() {
        Type t = getClass().getGenericSuperclass();
        if (t instanceof ParameterizedType parameterizedType) {
            Type[] actualTypeArguments = parameterizedType.getActualTypeArguments();
            if(actualTypeArguments.length != 1) {
                throw new FrameworkException(ExceptionType.JSON, Constants.UNREACHED);
            }
            this.type = actualTypeArguments[0];
        }else {
            throw new JsonParseException(Constants.UNREACHED);
        }
    }

    public Type type() {
        return type;
    }
}
