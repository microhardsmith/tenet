package cn.zorcc.log;

import lombok.extern.slf4j.Slf4j;
import org.junit.Test;

import java.io.File;
import java.io.IOException;
import java.io.RandomAccessFile;

@Slf4j
public class LogTest {
    @Test
    public void testLog() {
        for(int i = 0; i < 1000; i++) {
            log.info("hello world");
        }
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
