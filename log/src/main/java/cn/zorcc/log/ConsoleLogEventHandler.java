package cn.zorcc.log;

import cn.zorcc.common.Constants;
import cn.zorcc.common.WriteBuffer;
import cn.zorcc.common.enums.ExceptionType;
import cn.zorcc.common.event.EventHandler;
import cn.zorcc.common.exception.FrameworkException;
import cn.zorcc.common.util.NativeUtil;

import java.lang.foreign.FunctionDescriptor;
import java.lang.foreign.MemorySegment;
import java.lang.foreign.ValueLayout;
import java.lang.invoke.MethodHandle;
import java.nio.charset.StandardCharsets;
import java.util.function.BiConsumer;

/**
 *   print log to the console
 *   Using fputs and fflush from C to replace Java's PrintStream regarding to benchmark, could be several times faster that normal System.out.println():
 *      PrintTest.testNative  avgt   25   759.945 ±  28.064  ns/op
 *      PrintTest.testSout    avgt   25  2758.746 ± 108.389  ns/op
 */
public class ConsoleLogEventHandler implements EventHandler<LogEvent> {
    private final MethodHandle puts;
    private final MethodHandle fflush;
    private final BiConsumer<WriteBuffer, LogEvent> consumer;
    private final WriteBuffer buffer;

    public ConsoleLogEventHandler(LogConfig logConfig) {
        this.puts = NativeUtil.getNativeMethodHandle("puts", FunctionDescriptor.of(ValueLayout.JAVA_INT, ValueLayout.ADDRESS));
        this.fflush = NativeUtil.getNativeMethodHandle("fflush", FunctionDescriptor.of(ValueLayout.JAVA_INT, ValueLayout.ADDRESS));
        this.consumer = parseLogFormat(logConfig);
        // Console builder should have a larger size than logEvent
        this.buffer = new WriteBuffer(logConfig.getBufferSize() << 1);
    }

    /**
     *  向标准输出流打印数据,因为C风格的字符串以'\0'结尾,覆写内存后需要手动添加'\0'
     */
    private void print(WriteBuffer buffer) {
        try{
            buffer.writeByte(Constants.NUT);
            MemorySegment segment = buffer.segment();
            int p = (int) puts.invokeExact(segment);
            if(p < 0) {
                throw new FrameworkException(ExceptionType.LOG, "Failed to call puts() method");
            }
            int f = (int) fflush.invokeExact(NativeUtil.stdout());
            if(f != 0) {
                throw new FrameworkException(ExceptionType.LOG, "Failed to call fflush method");
            }
            buffer.reset();
        }catch (Throwable throwable) {
            throw new FrameworkException(ExceptionType.LOG, "Exception caught when invoking print()", throwable);
        }
    }

    /**
     *   get the color byte array that belong to the level bytes
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
     *   get the color byte array that belong to the color string
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
        if(!event.flush()) {
            consumer.accept(buffer, event);
            print(buffer);
            byte[] throwable = event.throwable();
            if(throwable != null) {
                buffer.writeBytes(throwable);
                print(buffer);
            }
        }
    }

    /**
     *   parsing log-format to lambda consumer
     */
    private static BiConsumer<WriteBuffer, LogEvent> parseLogFormat(LogConfig logConfig) {
        BiConsumer<WriteBuffer, LogEvent> result = (writeBuffer, logEvent) -> {};
        byte[] bytes = logConfig.getLogFormat().getBytes(StandardCharsets.UTF_8);
        for(int i = 0; i < bytes.length; i++) {
            byte b = bytes[i];
            if(b == Constants.b10) {
                int j = i + 1;
                for( ; ; ) {
                    if(bytes[j] == Constants.b10) {
                        String s = new String(bytes, i + 1, j - i - 1);
                        switch (s) {
                            case "time" -> {
                                byte[] timeColorBytes = colorBytes(logConfig.getTimeColor());
                                if(timeColorBytes != null) {
                                    result = result.andThen((writeBuffer, logEvent) -> {
                                        writeBuffer.writeBytes(Constants.ANSI_PREFIX);
                                        writeBuffer.writeBytes(timeColorBytes);
                                        writeBuffer.writeBytes(logEvent.time());
                                        writeBuffer.writeBytes(Constants.ANSI_SUFFIX);
                                    });
                                }else {
                                    result = result.andThen((writeBuffer, logEvent) -> writeBuffer.writeBytes(logEvent.time()));
                                }
                            }
                            case "level" -> result = result.andThen((writeBuffer, logEvent) -> {
                                byte[] level = logEvent.level();
                                writeBuffer.writeBytes(Constants.ANSI_PREFIX);
                                writeBuffer.writeBytes(levelColorBytes(level));
                                writeBuffer.writeBytes(level, logConfig.getLevelLen());
                                writeBuffer.writeBytes(Constants.ANSI_SUFFIX);
                            });
                            case "className" -> {
                                byte[] classNameColorBytes = colorBytes(logConfig.getClassNameColor());
                                if(classNameColorBytes != null) {
                                    result = result.andThen((writeBuffer, logEvent) -> {
                                        writeBuffer.writeBytes(Constants.ANSI_PREFIX);
                                        writeBuffer.writeBytes(classNameColorBytes);
                                        writeBuffer.writeBytes(logEvent.className(), logConfig.getClassNameLen());
                                        writeBuffer.writeBytes(Constants.ANSI_SUFFIX);
                                    });
                                }else {
                                    result = result.andThen((writeBuffer, logEvent) -> writeBuffer.writeBytes(logEvent.className(), logConfig.getClassNameLen()));
                                }
                            }
                            case "threadName" -> {
                                byte[] threadNameColorBytes = colorBytes(logConfig.getThreadNameColor());
                                if(threadNameColorBytes != null) {
                                    result = result.andThen((writeBuffer, logEvent) -> {
                                        writeBuffer.writeBytes(Constants.ANSI_PREFIX);
                                        writeBuffer.writeBytes(threadNameColorBytes);
                                        writeBuffer.writeBytes(logEvent.threadName(), logConfig.getThreadNameLen());
                                        writeBuffer.writeBytes(Constants.ANSI_SUFFIX);
                                    });
                                }else {
                                    result = result.andThen((writeBuffer, logEvent) -> writeBuffer.writeBytes(logEvent.threadName(), logConfig.getThreadNameLen()));
                                }
                            }
                            case "msg" -> {
                                byte[] msgColorBytes = colorBytes(logConfig.getMsgColor());
                                if(msgColorBytes != null) {
                                    result = result.andThen((writeBuffer, logEvent) -> {
                                        writeBuffer.writeBytes(Constants.ANSI_PREFIX);
                                        writeBuffer.writeBytes(msgColorBytes);
                                        writeBuffer.writeBytes(logEvent.msg());
                                        writeBuffer.writeBytes(Constants.ANSI_SUFFIX);
                                    });
                                }else {
                                    result = result.andThen((writeBuffer, logEvent) -> writeBuffer.writeBytes(logEvent.msg()));
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
                result = result.andThen((writeBuffer, logEvent) -> writeBuffer.writeByte(b));
            }
        }
        // the puts method in C will automatically add \n for our output
        return result;
    }
}
