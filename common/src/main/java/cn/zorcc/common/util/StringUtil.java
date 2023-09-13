package cn.zorcc.common.util;

import cn.zorcc.common.Constants;

import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.function.Consumer;

public final class StringUtil {
    private StringUtil() {
        throw new UnsupportedOperationException();
    }

    public static int searchBytes(byte[] data, byte expected, int startIndex, Consumer<byte[]> consumer) {
        for(int i = startIndex; i < data.length; i++) {
            if(data[i] == expected) {
                if(i > startIndex) {
                    consumer.accept(Arrays.copyOfRange(data, startIndex, i));
                }
                return i + Constants.ONE == data.length ? Integer.MIN_VALUE : i + Constants.ONE;
            }
        }
        return Integer.MIN_VALUE;
    }

    public static int searchStr(byte[] data, byte expected, int startIndex, Consumer<String> consumer) {
        for(int i = startIndex; i < data.length; i++) {
            if(data[i] == expected) {
                if(i > startIndex) {
                    consumer.accept(new String(data, startIndex, i - startIndex, StandardCharsets.UTF_8));
                }
                return i + Constants.ONE == data.length ? Integer.MIN_VALUE : i + Constants.ONE;
            }
        }
        return Integer.MIN_VALUE;
    }
}
