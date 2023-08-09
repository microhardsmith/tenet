package cn.zorcc.common.log;

import cn.zorcc.common.Constants;
import cn.zorcc.common.ReservedWriteBufferPolicy;
import cn.zorcc.common.ResizableByteArray;
import cn.zorcc.common.WriteBuffer;
import cn.zorcc.common.enums.ExceptionType;
import cn.zorcc.common.event.EventHandler;
import cn.zorcc.common.exception.FrameworkException;
import cn.zorcc.common.network.Native;
import cn.zorcc.common.util.NativeUtil;

import java.lang.foreign.*;
import java.lang.invoke.MethodHandle;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;

/**
 *   Print log to the console, using C's puts and fflush mechanism
 *   Using fputs and fflush from C to replace Java's PrintStream regarding to benchmark, could be several times faster that normal System.out.println():
 *      PrintTest.testNative  avgt   25   759.945 ±  28.064  ns/op
 *      PrintTest.testSout    avgt   25  2758.746 ± 108.389  ns/op
 */
public final class ConsoleLogEventHandler implements EventHandler<LogEvent> {
    /**
     *  Corresponding to `void g_print(char* str, FILE* stream)`
     */
    private final MethodHandle print;
    private final List<ConsoleConsumer> consumers;
    private final Arena arena;
    private final WriteBuffer buffer;
    private static final MemorySegment stdout = NativeUtil.stdout();
    private static final MemorySegment stderr = NativeUtil.stderr();

    public ConsoleLogEventHandler(LogConfig logConfig) {
        SymbolLookup symbolLookup = NativeUtil.loadLibrary(Native.LIB);
        this.print = NativeUtil.methodHandle(symbolLookup, "g_print", FunctionDescriptor.ofVoid(ValueLayout.ADDRESS, ValueLayout.ADDRESS));
        this.consumers = createConsoleConsumer(logConfig);
        // Console builder should have a larger size than logEvent, the constructor method should be guaranteed to be called in log thread to keep arena safe
        this.arena = Arena.ofConfined();
        this.buffer = new WriteBuffer(arena.allocateArray(ValueLayout.JAVA_BYTE, logConfig.getBufferSize() << 1), new ReservedWriteBufferPolicy());
    }

    /**
     *  Print to the console, note that C style string needs to manually add a '\0' to the end
     */
    private void print(MemorySegment stream) {
        try{
            MemorySegment content = buffer.content();
            print.invokeExact(content, stream);
            buffer.close();
        }catch (Throwable throwable) {
            throw new FrameworkException(ExceptionType.LOG, "Exception caught when invoking print()", throwable);
        }
    }

    /**
     *   Get the color byte array that belong to the level bytes
     */
    private static byte[] levelColorBytes(byte[] level) {
        if(level == Constants.ERROR_BYTES) {
            return Constants.RED_BYTES;
        }else if(level == Constants.WARN_BYTES) {
            return Constants.YELLOW_BYTES;
        }else {
            return Constants.GREEN_BYTES;
        }
    }

    /**
     *   Get the color byte array that belong to the color string
     */
    private static byte[] colorBytes(String color) {
        switch (color) {
            case Constants.RED -> {
                return Constants.RED_BYTES;
            }
            case Constants.GREEN -> {
                return Constants.GREEN_BYTES;
            }
            case Constants.YELLOW -> {
                return Constants.YELLOW_BYTES;
            }
            case Constants.BLUE -> {
                return Constants.BLUE_BYTES;
            }
            case Constants.MAGENTA -> {
                return Constants.MAGENTA_BYTES;
            }
            case Constants.CYAN -> {
                return Constants.CYAN_BYTES;
            }
            default -> {
                // no color wrapped
                return null;
            }
        }
    }

    @Override
    public void handle(LogEvent event) {
        // Ignore flush or shutdown since the console will be flushed every time
        if(event == LogEvent.shutdownEvent) {
            arena.close();
        }else if(event != LogEvent.flushEvent) {
            for (ConsoleConsumer consumer : consumers) {
                consumer.accept(buffer, event);
            }
            buffer.writeByte(Constants.LF);
            buffer.writeByte(Constants.NUT);
            print(stdout);
            byte[] throwable = event.throwable();
            if(throwable != null) {
                buffer.writeBytes(throwable);
                buffer.writeByte(Constants.NUT);
                print(stderr);
            }
        }
    }

    @FunctionalInterface
    interface ConsoleConsumer {
        void accept(WriteBuffer buffer, LogEvent event);
    }

    /**
     *   Parsing log-format to a lambda consumer list
     */
    private static List<ConsoleConsumer> createConsoleConsumer(LogConfig logConfig) {
        List<ConsoleConsumer> result = new ArrayList<>();
        ResizableByteArray arr = new ResizableByteArray(Constants.KB);
        byte[] bytes = logConfig.getLogFormat().getBytes(StandardCharsets.UTF_8);
        for(int i = 0; i < bytes.length; i++) {
            byte b = bytes[i];
            if(b == Constants.b10) {
                if(arr.writeIndex() > 0) {
                    final byte[] array = arr.toArray();
                    result.add((writeBuffer, logEvent) -> writeBuffer.writeBytes(array));
                    arr.reset();
                }
                int j = i + 1;
                for( ; ; ) {
                    if(bytes[j] == Constants.b10) {
                        String s = new String(bytes, i + 1, j - i - 1);
                        switch (s) {
                            case "time" -> {
                                byte[] timeColorBytes = colorBytes(logConfig.getTimeColor());
                                if(timeColorBytes != null) {
                                    result.add((writeBuffer, logEvent) -> {
                                        writeBuffer.writeBytes(Constants.ANSI_PREFIX);
                                        writeBuffer.writeBytes(timeColorBytes);
                                        writeBuffer.writeBytes(logEvent.time());
                                        writeBuffer.writeBytes(Constants.ANSI_SUFFIX);
                                    });
                                }else {
                                    result.add((writeBuffer, logEvent) -> writeBuffer.writeBytes(logEvent.time()));
                                }
                            }
                            case "level" -> result.add((writeBuffer, logEvent) -> {
                                byte[] level = logEvent.level();
                                writeBuffer.writeBytes(Constants.ANSI_PREFIX);
                                writeBuffer.writeBytes(levelColorBytes(level));
                                writeBuffer.writeBytes(level, logConfig.getLevelLen());
                                writeBuffer.writeBytes(Constants.ANSI_SUFFIX);
                            });
                            case "className" -> {
                                byte[] classNameColorBytes = colorBytes(logConfig.getClassNameColor());
                                if(classNameColorBytes != null) {
                                    result.add((writeBuffer, logEvent) -> {
                                        writeBuffer.writeBytes(Constants.ANSI_PREFIX);
                                        writeBuffer.writeBytes(classNameColorBytes);
                                        writeBuffer.writeBytes(logEvent.className(), logConfig.getClassNameLen());
                                        writeBuffer.writeBytes(Constants.ANSI_SUFFIX);
                                    });
                                }else {
                                    result.add((writeBuffer, logEvent) -> writeBuffer.writeBytes(logEvent.className(), logConfig.getClassNameLen()));
                                }
                            }
                            case "threadName" -> {
                                byte[] threadNameColorBytes = colorBytes(logConfig.getThreadNameColor());
                                if(threadNameColorBytes != null) {
                                    result.add((writeBuffer, logEvent) -> {
                                        writeBuffer.writeBytes(Constants.ANSI_PREFIX);
                                        writeBuffer.writeBytes(threadNameColorBytes);
                                        writeBuffer.writeBytes(logEvent.threadName(), logConfig.getThreadNameLen());
                                        writeBuffer.writeBytes(Constants.ANSI_SUFFIX);
                                    });
                                }else {
                                    result.add((writeBuffer, logEvent) -> writeBuffer.writeBytes(logEvent.threadName(), logConfig.getThreadNameLen()));
                                }
                            }
                            case "msg" -> {
                                byte[] msgColorBytes = colorBytes(logConfig.getMsgColor());
                                if(msgColorBytes != null) {
                                    result.add((writeBuffer, logEvent) -> {
                                        writeBuffer.writeBytes(Constants.ANSI_PREFIX);
                                        writeBuffer.writeBytes(msgColorBytes);
                                        writeBuffer.writeBytes(logEvent.msg());
                                        writeBuffer.writeBytes(Constants.ANSI_SUFFIX);
                                    });
                                }else {
                                    result.add((writeBuffer, logEvent) -> writeBuffer.writeBytes(logEvent.msg()));
                                }
                            }
                            default -> throw new FrameworkException(ExceptionType.LOG, "Unresolved log format : %s".formatted(s));
                        }
                        break;
                    }else if(++j == bytes.length){
                        throw new FrameworkException(ExceptionType.LOG, "LogFormat corrupted");
                    }
                }
                i = j;
            }else {
                arr.write(b);
            }
        }
        if(arr.writeIndex() > 0) {
            final byte[] array = arr.toArray();
            result.add((writeBuffer, logEvent) -> writeBuffer.writeBytes(array));
            arr.reset();
        }
        // the puts method in C will automatically add \n for our output
        return result;
    }
}
