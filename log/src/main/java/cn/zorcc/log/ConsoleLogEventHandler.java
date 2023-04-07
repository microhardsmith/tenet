package cn.zorcc.log;

import cn.zorcc.common.Constants;
import cn.zorcc.common.WriteBuffer;
import cn.zorcc.common.enums.ExceptionType;
import cn.zorcc.common.event.EventHandler;
import cn.zorcc.common.exception.FrameworkException;
import cn.zorcc.common.util.NativeUtil;

import java.lang.foreign.FunctionDescriptor;
import java.lang.foreign.MemorySegment;
import java.lang.foreign.SymbolLookup;
import java.lang.foreign.ValueLayout;
import java.lang.invoke.MethodHandle;
import java.nio.charset.StandardCharsets;
import java.util.function.BiConsumer;

/**
 * 将日志打印至控制台
 */
public class ConsoleLogEventHandler implements EventHandler<LogEvent> {
    private final MethodHandle printHandle;
    private final BiConsumer<WriteBuffer, LogEvent> consumer;
    private final WriteBuffer buffer;

    public ConsoleLogEventHandler(LogConfig logConfig) {
        SymbolLookup symbolLookup = NativeUtil.loadLibraryFromResource(NativeUtil.commonLib());
        this.printHandle = NativeUtil.methodHandle(symbolLookup, "g_print", FunctionDescriptor.ofVoid(ValueLayout.ADDRESS));
        this.consumer = parseLogFormat(logConfig);
        // Console builder should have a larger size than logEvent
        this.buffer = new WriteBuffer(logConfig.getBufferSize() << 1);
    }

    /**
     *  向标准输出流打印数据
     */
    private void print(WriteBuffer buffer) {
        try{
            MemorySegment segment = buffer.segment();
            printHandle.invokeExact(segment);
            buffer.reset();
        }catch (Throwable throwable) {
            throw new FrameworkException(ExceptionType.LOG, "Exception caught when invoking print()", throwable);
        }
    }

    private static byte[] levelBytes(byte[] level) {
        if(level == Constants.ERROR_BYTES) {
            return Constants.RED_BYTES;
        }else if(level == Constants.WARN_BYTES) {
            return Constants.YELLOW_BYTES;
        }else {
            return Constants.GREEN_BYTES;
        }
    }

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
     *   解析日志格式，生成lambda处理方法
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
                                writeBuffer.writeBytes(levelBytes(level));
                                writeBuffer.writeBytes(level);
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
