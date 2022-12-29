package cn.zorcc.common.serializer;

import cn.zorcc.common.Constants;
import cn.zorcc.common.enums.ExceptionType;
import cn.zorcc.common.exception.FrameworkException;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.fasterxml.jackson.datatype.jsr310.deser.LocalDateTimeDeserializer;
import com.fasterxml.jackson.datatype.jsr310.ser.LocalDateTimeSerializer;

import java.io.IOException;
import java.text.SimpleDateFormat;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

/**
 * jackson序列化,虽然objectMapper是线程安全的,但是不建议在多线程环境下使用,会有很严重的锁开销
 */
public class JsonSerializer implements Serializer {

    private static final ObjectMapper objectMapper = createObjectMapper();

    public static ObjectMapper createObjectMapper() {
        ObjectMapper objectMapper = new ObjectMapper();
        // 转换为格式化的json
        objectMapper.enable(SerializationFeature.INDENT_OUTPUT);
        //若POJO对象的属性值为null,序列化时不进行显示
        objectMapper.setSerializationInclusion(JsonInclude.Include.NON_NULL);
        //若POJO对象的属性值为"",序列化时不进行显示
        objectMapper.setSerializationInclusion(JsonInclude.Include.NON_EMPTY);
        //配置默认的时间格式
        objectMapper.setDateFormat(new SimpleDateFormat(Constants.DATE_FORMAT));
        //使用LocalDateTime而不是Date类来表示时间
        objectMapper.disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);
        // 如果json中有新增的字段并且是实体类类中不存在的,不报错
        objectMapper.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
        //针对于JDK新时间类。序列化时带有T的问题,自定义格式化字符串
        JavaTimeModule javaTimeModule = new JavaTimeModule();
        javaTimeModule.addSerializer(LocalDateTime.class, new LocalDateTimeSerializer(DateTimeFormatter.ofPattern(Constants.DATE_FORMAT)));
        javaTimeModule.addDeserializer(LocalDateTime.class, new LocalDateTimeDeserializer(DateTimeFormatter.ofPattern(Constants.DATE_FORMAT)));
        objectMapper.registerModule(javaTimeModule);
        return objectMapper;
    }

    @Override
    public byte[] serialize(Object obj) {
        try {
            return objectMapper.writeValueAsBytes(obj);
        } catch (JsonProcessingException e) {
            throw new FrameworkException(ExceptionType.JSON, "Can't serialize obj into bytes : " + obj.getClass().getSimpleName(), e);
        }
    }

    @Override
    public <T> T deserialize(byte[] bytes, Class<T> clazz) {
        try {
            return objectMapper.readValue(bytes, clazz);
        } catch (IOException e) {
            throw new FrameworkException(ExceptionType.JSON, "Can't deserialize bytes into obj, class : " + clazz.getSimpleName(), e);
        }
    }
}
