package cn.zorcc.log;

import cn.zorcc.common.Constants;
import lombok.Getter;
import lombok.Setter;

/**
 * 日志配置文件,默认从当前工程resources目录下寻找log-config.json读取
 */
@Getter
@Setter
public class LogConfig {
    /**
     * 日志默认允许输出的最低级别,与Slf4j规定的日志级别吻合
     */
    private int level = Constants.DEBUG;
    /**
     * 日志队列大小
     */
    private int queueSize = 1 << 10;
    /**
     * 是否将日志输出到控制台,默认为true,基于性能考虑可以考虑关闭该项(输出至文件会比输出至控制台快许多)
     */
    private boolean usingConsole = true;
    /**
     *  日志等级Console输出预留长度
     */
    private int levelLen = 5;
    /**
     *  线程名Console输出预留长度
     */
    private int threadNameLen = 15;
    /**
     *  类名Console输出预留长度
     */
    private int classNameLen = 60;
    /**
     *  打印日志时间的颜色
     */
    private String timeColor = Constants.BLUE;
    /**
     *  打印日志线程名的颜色
     */
    private String threadNameColor = Constants.MAGENTA;
    /**
     *  打印日志类名的颜色
     */
    private String classNameColor = Constants.CYAN;
    /**
     *  打印日志消息的颜色
     */
    private String msgColor = Constants.BLUE;
    /**
     * 是否将日志输出到文件,默认为false
     */
    private boolean usingFile = false;
    /**
     * 日志文件目录路径,如果未指定则使用项目目录下新建log目录存储
     */
    private String logFileDir = Constants.EMPTY_STRING;
    /**
     * 日志文件名称
     */
    private String logFileName = Constants.DEFAULT_LOG_FILE_NAME;
    /**
     * 日志文件大小限制设置,超过将重命名旧日志文件为当前时间,将新日志写入新文件中,单位KB
     */
    private int maxFileSize = 200 * 1024;
    /**
     *  是否启用滚动更新
     */
    private boolean enableRolling = false;
    /**
     *  允许产生的日志文件数,旧文件会以 logFileName-%d.log进行命名
     */
    private int rollingFileCount = 1;
    /**
     * 是否将日志传输至Metrics保存,默认为false
     */
    private boolean usingMetrics = false;
}
