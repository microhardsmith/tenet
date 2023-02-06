package cn.zorcc.log;

import cn.zorcc.common.Context;
import cn.zorcc.common.event.EventPipeline;
import cn.zorcc.common.pojo.Loc;
import lombok.extern.slf4j.Slf4j;
import org.junit.Test;

import java.io.File;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.lang.reflect.Field;

@Slf4j
public class LogTest {
    @Test
    public void testReflect() throws InterruptedException {
        Loc loc = new Loc("123",123);
        Class<? extends Loc> clazz = loc.getClass();
        for (Field declaredField : clazz.getDeclaredFields()) {
            System.out.println(declaredField.getName());
        }
    }

    public static void main(String[] args) throws InterruptedException {
        EventPipeline<LogEvent> pipeline = Context.pipeline(LogEvent.class);
        pipeline.init();
        for(int i = 0; i < 500000; i++) {
            log.info(String.valueOf(i));
        }
        Thread.sleep(5000L);
    }

    @Test
    public void testFile() throws IOException {
        File f = new File("test.log");
        if(!f.exists()) {
            f.createNewFile();
        }
        long l = System.currentTimeMillis();
        for(int i = 0; i < 1000; i++) {
            try(RandomAccessFile file = new RandomAccessFile("test.log", "rw")) {
                file.writeChars("hello");
            }
        }
        long l1 = System.currentTimeMillis();
        System.out.println(l1 - l);
        try(RandomAccessFile file = new RandomAccessFile("test.log", "rw")) {
            for(int i = 0; i < 1000; i++) {
                file.writeChars("hello");
            }
        }
        long l2 = System.currentTimeMillis();
        System.out.println(l2 - l1);
    }
}
