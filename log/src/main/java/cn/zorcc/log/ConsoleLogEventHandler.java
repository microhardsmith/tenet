package cn.zorcc.log;

import cn.zorcc.common.Constants;
import cn.zorcc.common.enums.ExceptionType;
import cn.zorcc.common.exception.FrameworkException;
import cn.zorcc.common.util.ThreadUtil;
import org.slf4j.event.Level;

import java.io.PrintStream;
import java.util.Map;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.LinkedBlockingQueue;

/**
 * 将日志打印至控制台
 */
public class ConsoleLogEventHandler implements cn.zorcc.common.event.EventHandler<LogEvent>, cn.zorcc.common.LifeCycle {
    /**
     *  系统输出流
     */
    private static final PrintStream outPrintStream = new PrintStream(System.out, true);
    /**
     *  缓存指定长度的空格数
     */
    private static final Map<Integer, String> blankMap = new ConcurrentHashMap<>();
    /**
     *  Ansi格式的带颜色的控制台字符控制
     */
    private static final String PREFIX = "\033[";
    private static final String SUFFIX = " \033[0m";
    /**
     *  日志等级Console输出预留长度
     */
    private final int levelLen;
    /**
     *  线程名Console输出预留长度
     */
    private final int threadNameLen;
    /**
     *  类名Console输出预留长度
     */
    private final int classNameLen;
    /**
     *  打印日志时间的颜色
     */
    private final String timeColor;
    /**
     *  打印日志线程名的颜色
     */
    private final String threadNameColor;
    /**
     *  打印日志类名的颜色
     */
    private final String classNameColor;
    /**
     *  打印日志消息的颜色
     */
    private final String msgColor;
    /**
     *  日志打印线程
     */
    private final Thread thread;
    /**
     *  日志消费队列
     */
    private final BlockingQueue<LogEvent> queue = new LinkedBlockingQueue<>();

    public ConsoleLogEventHandler(LogConfig logConfig) {
        this.levelLen = logConfig.getLevelLen();
        this.threadNameLen = logConfig.getThreadNameLen();
        this.classNameLen = logConfig.getClassNameLen();
        this.timeColor = logConfig.getTimeColor();
        this.threadNameColor = logConfig.getThreadNameColor();
        this.classNameColor = logConfig.getClassNameColor();
        this.msgColor = logConfig.getMsgColor();
        this.thread = ThreadUtil.virtual("logConsoleHandler", () -> {
            Thread currentThread = Thread.currentThread();
            while (!currentThread.isInterrupted()) {
                try{
                    LogEvent logEvent = queue.take();
                    String consoleMsg = createConsoleMsg(logEvent);
                    outPrintStream.print(consoleMsg);
                    Throwable throwable = logEvent.getThrowable();
                    if (throwable != null) {
                        throwable.printStackTrace(outPrintStream);
                    }
                }catch (InterruptedException e) {
                    currentThread.interrupt();
                }
            }
        });

    }

    @Override
    public void init() {
        thread.start();
    }

    @Override
    public void handle(LogEvent event) {
        try {
            queue.put(event);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new FrameworkException(ExceptionType.LOG, "Thread interrupt", e);
        }
    }

    @Override
    public void shutdown() {
        thread.interrupt();
    }

    /**
     *  构建控制台使用的日志消息,利用ansi给日志添加颜色
     */
    private String createConsoleMsg(LogEvent logEvent) {
        final StringBuilder lineSb = logEvent.getBuilder();
        wrap(lineSb, timeColor, logEvent.getLogTime().timeArray());
        lineSb.append(Constants.BLANK);
        Level level = logEvent.getLevel();
        wrap(lineSb, levelColor(level), level.name(), levelLen);
        lineSb.append(Constants.BLANK).append(Constants.L_SQUARE);
        wrap(lineSb, threadNameColor, logEvent.getThreadName(), threadNameLen);
        lineSb.append(Constants.R_SQUARE).append(Constants.BLANK);
        wrap(lineSb, classNameColor, logEvent.getClassName(), classNameLen);
        lineSb.append(Constants.HYPHEN);
        wrap(lineSb, msgColor, logEvent.getMsg());
        lineSb.append(Constants.LINE_SEPARATOR);
        return lineSb.toString();
    }

    /**
     * 使用Ansi color code将指定消息用特定颜色包裹添加至StringBuilder中
     * @param stringBuilder 需要进行Append的StringBuilder
     * @param color 颜色格式,可以使用空字符串来指定不使用任何颜色
     * @param msg 消息体
     * @param minLen 最小占据长度
     */
    private void wrap(StringBuilder stringBuilder, String color, String msg, int minLen) {
        wrap(stringBuilder, color, msg);
        int len = msg.length();
        if(len < minLen) {
            stringBuilder.append(blankMap.computeIfAbsent(minLen - len, Constants.BLANK::repeat));
        }
    }

    private void wrap(StringBuilder stringBuilder, String color, String msg) {
        if(!color.isBlank()) {
            stringBuilder.append(PREFIX).append(color).append(msg).append(SUFFIX);
        }else {
            stringBuilder.append(msg);
        }
    }

    private void wrap(StringBuilder stringBuilder, String color, char[] msg) {
        if(!color.isBlank()) {
            stringBuilder.append(PREFIX).append(color).append(msg).append(SUFFIX);
        }else {
            stringBuilder.append(msg);
        }
    }

    /**
     * 获取日志等级所对应的日志颜色
     * @param level 日志等级
     */
    private String levelColor(Level level) {
        switch (level) {
            case WARN -> {
                return Constants.YELLOW;
            }
            case ERROR -> {
                return Constants.RED;
            }
            default -> {
                return Constants.GREEN;
            }
        }
    }
}
