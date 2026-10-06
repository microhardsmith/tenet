package cn.zorcc.common.log;

import cn.zorcc.common.Context;
import cn.zorcc.common.LifeCycle;
import cn.zorcc.common.structure.Wheel;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;
import java.time.Instant;

import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;

/**
 *   Context.init() may only be invoked once per JVM, so the plain and the exception logging scenarios share a single
 *   test method. The assertions read the file sink back, which covers the log config load, WriteBuffer.toString() and
 *   FileLogEventHandler paths end to end.
 */
@Tag("container-log")
public class LogTest {
    private static final Logger log = new Logger(LogTest.class);
    private static final String INFO_MARKER = "tenet-log-marker-info";
    private static final String ERROR_MARKER = "tenet-log-marker-error";
    private static final String CAUSE_MARKER = "tenet-log-marker-cause";
    private static final Duration LOG_FLUSH_TIMEOUT = Duration.ofSeconds(10);

    @Test
    public void testLog() throws Exception {
        Context.init();
        try{
            log.info(INFO_MARKER);
            log.error(ERROR_MARKER, new RuntimeException(CAUSE_MARKER));

            String content = readLogFileContaining(INFO_MARKER);
            assertTrue(content.contains(INFO_MARKER), "Logged info message should reach the log sink");
            assertTrue(content.contains(ERROR_MARKER), "Logged error message should reach the log sink");
            assertTrue(content.contains(CAUSE_MARKER), "Exception stack trace should reach the log sink");
        }finally {
            exitQuietly("LoggerConsumer", Context.get(LoggerConsumer.class));
            exitQuietly("Wheel", Context.get(Wheel.class));
        }
    }

    /**
     *   The log consumer thread flushes on its own interval, so the file is polled until the marker shows up
     */
    private static String readLogFileContaining(String marker) throws IOException, InterruptedException {
        Path dir = resolveLogDir();
        Instant deadline = Instant.now().plus(LOG_FLUSH_TIMEOUT);
        IOException lastFailure = null;
        while(Instant.now().isBefore(deadline)) {
            if(Files.exists(dir)) {
                try (var paths = Files.list(dir)) {
                    for(Path path : paths.sorted().toList()) {
                        String content = Files.readString(path);
                        if(content.contains(marker)) {
                            return content;
                        }
                    }
                }catch (IOException e) {
                    lastFailure = e;
                }
            }
            Thread.sleep(100);
        }
        fail("No log file under " + dir + " contained " + marker + " within " + LOG_FLUSH_TIMEOUT
                + (lastFailure == null ? "" : ", last read failure : " + lastFailure), lastFailure);
        throw new IllegalStateException("unreachable");
    }

    private static Path resolveLogDir() {
        String configured = Logger.getLogConfig().getFile() == null ? null : Logger.getLogConfig().getFile().getDir();
        if(configured == null || configured.isBlank()) {
            configured = System.getProperty("user.dir");
        }
        Path dir = Path.of(configured);
        return dir.isAbsolute() ? dir : Path.of(System.getProperty("user.dir")).resolve(dir);
    }

    private static void exitQuietly(String name, LifeCycle lifeCycle) {
        if(lifeCycle == null) {
            return ;
        }
        try{
            lifeCycle.UninterruptibleExit();
        }catch (RuntimeException e) {
            System.err.println("Stopping " + name + " failed: " + e);
        }
    }
}