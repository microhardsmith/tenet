package cn.zorcc.common.json;

import cn.zorcc.common.ReadBuffer;
import cn.zorcc.common.WriteBuffer;
import cn.zorcc.common.beans.Book;
import cn.zorcc.common.beans.City;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.lang.foreign.MemorySegment;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Map;

public class JsonTest {
    @Test
    public void testCity() {
        City c1 = new City();
        c1.setName("New York");
        c1.setPopulation(8537673);
        c1.setArea(783.8);
        c1.setCapital(false);
        c1.setCategory('A');
        c1.setElevation(10);
        try(WriteBuffer writeBuffer = WriteBuffer.newHeapWriteBuffer()) {
            JsonParser.serializeAsObject(writeBuffer, c1);
            String serializeResult = writeBuffer.toString();
            System.out.println(serializeResult);
            City c2 = JsonParser.deserializeAsObject(new ReadBuffer(MemorySegment.ofArray(serializeResult.getBytes(StandardCharsets.UTF_8))), City.class);
            Assertions.assertEquals(c1, c2);
        }
    }

    @Test
    public void testBook() {
        Book b1 = new Book();
        b1.setId(Long.MAX_VALUE);
        b1.setNames(List.of("hello","world","goodbye"));
        b1.setMap(Map.of("a", List.of(1,2,3), "b", List.of(4,5,6)));
        try(WriteBuffer writeBuffer = WriteBuffer.newHeapWriteBuffer()) {
            JsonParser.serializeAsObject(writeBuffer, b1);
            String serializeResult = writeBuffer.toString();
            System.out.println(serializeResult);
            Book b2 = JsonParser.deserializeAsObject(new ReadBuffer(MemorySegment.ofArray(serializeResult.getBytes(StandardCharsets.UTF_8))), Book.class);
            Assertions.assertEquals(b1, b2);
        }
    }
}
