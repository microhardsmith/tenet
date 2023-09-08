package cn.zorcc.common.json;

import cn.zorcc.common.Constants;
import cn.zorcc.common.ReadBuffer;
import cn.zorcc.common.ResizableByteArray;
import cn.zorcc.common.exception.JsonParseException;

import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

public final class JsonReaderMapNode extends JsonReaderNode {
    private final ReadBuffer readBuffer;
    private final Map<String, Object> map = new HashMap<>();
    private final Class<?> type;
    private final Type valueType;
    private final ResizableByteArray writer = new ResizableByteArray();
    private String currentKey;

    public JsonReaderMapNode(ReadBuffer readBuffer, Class<?> type, Type valueType) {
        this.readBuffer = readBuffer;
        this.type = type;
        this.valueType = valueType;
    }

    @Override
    public JsonNode process() {
        byte fb = JsonParser.readNextByte(readBuffer);
        if(fb == Constants.RCB) {
            return toPrev();
        }else if(fb != Constants.COMMA) {
            throw new JsonParseException(readBuffer);
        }
        for( ; ; ) {
            JsonParser.readExpected(readBuffer, Constants.QUOTE);
            currentKey = JsonParser.readStringUntil(readBuffer, writer, Constants.QUOTE);
            JsonParser.readExpected(readBuffer, Constants.COLON);
            byte b = JsonParser.readNextByte(readBuffer);
            switch (b) {
                case Constants.LCB -> {
                    if(valueType instanceof Class<?> c) {
                        return toNext(new JsonReaderObjectNode(readBuffer, c));
                    }else if(valueType instanceof ParameterizedType parameterizedType && parameterizedType.getRawType() instanceof Class<?> c && Map.class.isAssignableFrom(c)) {
                        Type[] actualTypeArguments = parameterizedType.getActualTypeArguments();
                        if(actualTypeArguments[Constants.ZERO] instanceof Class<?> keyClass && keyClass == String.class) {
                            return toNext(new JsonReaderMapNode(readBuffer, c, actualTypeArguments[Constants.ONE]));
                        }else {
                            throw new JsonParseException(Constants.JSON_KEY_TYPE_ERR);
                        }
                    }else {
                        throw new JsonParseException(Constants.JSON_VALUE_TYPE_ERR);
                    }
                }
                case Constants.LSB -> {
                    if(valueType instanceof Class<?> c && c.isArray()) {
                        return toNext(new JsonReaderCollectionNode(readBuffer, c, c.componentType()));
                    }else if(valueType instanceof ParameterizedType pt && pt.getRawType() instanceof Class<?> c && Collection.class.isAssignableFrom(c)) {
                        return toNext(new JsonReaderCollectionNode(readBuffer, c, pt.getActualTypeArguments()[Constants.ZERO]));
                    }else {
                        throw new JsonParseException(Constants.JSON_VALUE_TYPE_ERR);
                    }
                }
                case Constants.QUOTE -> {
                    if(valueType instanceof Class<?> c && c == String.class) {
                        JsonParser.readStringUntil(readBuffer, writer, Constants.QUOTE);
                        map.put(currentKey, writer.asString());
                        byte sep = JsonParser.readNextByte(readBuffer);
                        if(sep == Constants.RCB) {
                            return toPrev();
                        }else if(sep != Constants.COMMA) {
                            throw new JsonParseException(readBuffer);
                        }
                    }else {
                        throw new JsonParseException(Constants.JSON_VALUE_TYPE_ERR);
                    }
                }
                case (byte) 'n' -> {
                    JsonParser.readFollowingNullValue(readBuffer);
                    map.put(currentKey, null);
                }
                case (byte) 't' -> {
                    if(valueType instanceof Class<?> c && c == Boolean.class) {
                        JsonParser.readFollowingTrueValue(readBuffer);
                        map.put(currentKey, Boolean.TRUE);
                    }else {
                        throw new JsonParseException(Constants.JSON_VALUE_TYPE_ERR);
                    }
                }
                case (byte) 'f' -> {
                    if(valueType instanceof Class<?> c && c == Boolean.class) {
                        JsonParser.readFollowingFalseValue(readBuffer);
                        map.put(currentKey, Boolean.FALSE);
                    }else {
                        throw new JsonParseException(Constants.JSON_VALUE_TYPE_ERR);
                    }
                }
                default -> {
                    if(valueType instanceof Class<?> c && (c.isPrimitive() || Number.class.isAssignableFrom(c))) {
                        byte sep = JsonParser.readUntilMatch(readBuffer, writer, Constants.COMMA, Constants.RCB);
                        map.put(currentKey, JsonParser.convertJsonNumberValue(c, writer.asString()));
                        if(sep == Constants.RCB) {
                            return toPrev();
                        }else if(sep != Constants.COMMA) {
                            throw new JsonParseException(readBuffer);
                        }
                    }else {
                        throw new JsonParseException(Constants.JSON_VALUE_TYPE_ERR);
                    }
                }
            }
        }
    }

    @Override
    public void assign(Object value) {
        map.put(currentKey, value);
    }

    @Override
    public Object construct() {
        return JsonParser.convertJsonMapValue(type, map);
    }
}
