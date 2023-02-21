package cn.zorcc.log;

import cn.zorcc.common.BlockingQ;
import cn.zorcc.common.Constants;
import cn.zorcc.common.MpscBlockingQ;
import cn.zorcc.common.event.EventHandler;
import org.slf4j.event.Level;

import java.io.PrintStream;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 将日志打印至控制台
 */
public class ConsoleLogEventHandler implements EventHandler<LogEvent> {
    private static final String NAME = "logConsoleHandler";
    /**
     *  系统输出流
     */
    private static final PrintStream outPrintStream = System.out;
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
     *  日志消费者阻塞队列
     */
    private final BlockingQ<LogEvent> blockingQ;

    public ConsoleLogEventHandler(LogConfig logConfig) {
        this.levelLen = logConfig.getLevelLen();
        this.threadNameLen = logConfig.getThreadNameLen();
        this.classNameLen = logConfig.getClassNameLen();
        this.timeColor = logConfig.getTimeColor();
        this.threadNameColor = logConfig.getThreadNameColor();
        this.classNameColor = logConfig.getClassNameColor();
        this.msgColor = logConfig.getMsgColor();
        this.blockingQ = new MpscBlockingQ<>(NAME, logEvent -> {
            if(logEvent.isFlush()) {

            }else {

            }
            String consoleMsg = createConsoleMsg(logEvent);
            outPrintStream.print(consoleMsg);
            Throwable throwable = logEvent.getThrowable();
            if (throwable != null) {
                throwable.printStackTrace(outPrintStream);
            }
        });
    }

    @Override
    public void init() {
        blockingQ.start();
    }

    @Override
    public void handle(LogEvent event) {
        blockingQ.put(event);
    }

    @Override
    public void shutdown() {
        blockingQ.shutdown();
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
        if(!color.isEmpty()) {
            stringBuilder.append(PREFIX).append(color).append(msg).append(SUFFIX);
        }else {
            stringBuilder.append(msg);
        }
    }

    private void wrap(StringBuilder stringBuilder, String color, char[] msg) {
        if(!color.isEmpty()) {
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
