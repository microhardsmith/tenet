package cn.zorcc.common.json;

import cn.zorcc.common.ResizableByteArray;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

public class JsonWriterTest {
    @Test
    public void testJsonSerialize() {
        JsonBean jsonBean = new JsonBean();
        jsonBean.setStrValue("hello world");
        ResizableByteArray resizableByteArray = new ResizableByteArray();
        JsonParser.serializeObject(resizableByteArray, jsonBean);
        Assertions.assertTrue(resizableByteArray.asString().contains("hello world"));
    }
}
