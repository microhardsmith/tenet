package cn.zorcc.common.json;

import cn.zorcc.common.FieldAccess;
import com.fasterxml.jackson.core.JsonFactory;
import com.fasterxml.jackson.core.JsonGenerator;
import org.junit.jupiter.api.Test;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;

public class JsonTest {
    @Test
    public void testFile() throws IOException {
        try(InputStream stream = this.getClass().getResourceAsStream("/test.json")) {
            assert stream != null;
            int available = stream.available();
            System.out.println("available : " + available);
            int times = (available / 10) + 1;
            for(int i = 0; i < times; i++) {
                byte[] bytes = new byte[10];
                System.out.println("read : " + stream.read(bytes));
                System.out.println("str" + new String(bytes, StandardCharsets.UTF_8));
                System.out.println(Arrays.toString(bytes));
            }
        }
    }

    @Test
    public void testAccessObj() {
        FieldAccess fieldAccess = FieldAccess.get(PlainObj.class);
        System.out.println(fieldAccess.access("v1"));
    }

    @Test
    public void testAccessRecord() {
        FieldAccess fieldAccess = FieldAccess.get(PlainRecord.class);
        System.out.println(fieldAccess.access("v2"));
    }

    @Test
    public void testJackson() throws IOException {
        JsonFactory jsonFactory = new JsonFactory();
        ByteArrayOutputStream stream = new ByteArrayOutputStream();
        JsonGenerator jGenerator = jsonFactory.createGenerator(stream);
        jGenerator.writeStartObject();
        jGenerator.writeStringField("name", "张三");
        jGenerator.writeNumberField("age", 25);
        jGenerator.writeFieldName("address");
        jGenerator.writeStartArray();
        jGenerator.writeString("Poland");
        jGenerator.writeString("5th avenue");
        jGenerator.writeEndArray();
        jGenerator.writeEndObject();
        jGenerator.close();
        System.out.println(stream.toString(StandardCharsets.UTF_8));
    }

}
