package cn.zorcc.common;

import cn.zorcc.common.exception.FrameworkException;
import cn.zorcc.common.util.NativeUtil;

import java.lang.foreign.MemorySegment;
import java.lang.foreign.ValueLayout;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.time.Clock;
import java.time.OffsetTime;
import java.time.ZoneOffset;

/**
 *   Constants pool
 */
public final class Constants {
    /**
     *   dynamic library
     */
    public static final String LIB = "lib";
    public static final String TENET = "libtenet";
    public static final String CRYPTO = "libcrypto";
    public static final String SSL = "libssl";
    public static final String DEFLATE = "libdeflate";
    public static final String BROTLI_COMMON = "libbrotlicommon";
    public static final String BROTLI_ENC = "libbrotlienc";
    public static final String BROTLI_DEC = "libbrotildec";
    public static final String SQLITE = "libsqlite";
    public static final String DUCKDB = "libduckdb";
    public static final String ALLOCATOR = "allocator";
    public static final String TENET_LIBRARY_PATH = "TENET_LIBRARY_PATH";
    public static final String CA_FILE = "CA_FILE";
    public static final String CA_DIR = "CA_DIR";
    public static final String GET = "get";
    public static final String SET = "set";
    public static final String IS = "is";
    public static final String APPLY = "apply";
    public static final String ACCEPT = "accept";
    public static final int INITIAL = 0;
    public static final int RUNNING = 1;
    public static final int CLOSING = 2;
    public static final int STOPPED = 3;
    public static final String DEFAULT_TIME_FORMAT = "yyyy-MM-dd HH:mm:ss.SSS";
    public static final Clock SYSTEM_CLOCK = Clock.systemDefaultZone();
    public static final ZoneOffset LOCAL_ZONE_OFFSET = OffsetTime.now().getOffset();

    /**
     *   Byte size for basic value types
     */
    public static final long BYTE_SIZE = ValueLayout.JAVA_BYTE.byteSize();
    public static final long SHORT_SIZE = ValueLayout.JAVA_SHORT.byteSize();
    public static final long INT_SIZE = ValueLayout.JAVA_INT.byteSize();
    public static final long LONG_SIZE = ValueLayout.JAVA_LONG.byteSize();
    public static final long FLOAT_SIZE = ValueLayout.JAVA_FLOAT.byteSize();
    public static final long DOUBLE_SIZE = ValueLayout.JAVA_DOUBLE.byteSize();

    /**
     *   Ascii byte value
     */
    public static final byte HYPHEN = (byte) '-';
    public static final byte SPACE = (byte) ' ';
    public static final byte COLON = (byte) ':';
    public static final byte QUOTE = (byte) '"';
    public static final byte PERIOD = (byte) '.';
    public static final byte COMMA = (byte) ',';
    public static final byte LCB = (byte) '{';
    public static final byte RCB = (byte) '}';
    public static final byte LSB = (byte) '[';
    public static final byte RSB = (byte) ']';
    public static final byte BS = (byte) '\b';
    public static final byte FF = (byte) '\f';
    public static final byte HT = (byte) '\t';
    public static final byte CR = (byte) '\r';
    public static final byte LF = (byte) '\n';
    public static final byte PERCENT = (byte) '%';
    public static final byte NUT = (byte) '\0';
    public static final byte B_ZERO = (byte) '0';
    public static final byte B_NINE = (byte) '9';
    public static final byte B_A = (byte) 'A';
    public static final byte B_F = (byte) 'F';
    public static final byte B_a = (byte) 'a';
    public static final byte B_f = (byte) 'f';
    public static final byte ESCAPE = (byte) '\\';
    public static final byte UNICODE_CODE = (byte) 'u';
    public static final byte SLASH = (byte) '/';
    public static final byte DEL = (byte) 0x7F;

    /**
     *   HTTP
     */
    public static final String DEFAULT_HTTP_VERSION = "HTTP/1.1";
    public static final byte[] HTTP_LINE_SEP = new byte[]{CR, LF};
    public static final byte[] HTTP_PAIR_SEP = new byte[]{COLON, SPACE};

    /**
     *   LOG
     */

    public static final int TRACE = 0;
    public static final int DEBUG = 10;
    public static final int INFO = 20;
    public static final int WARN = 30;
    public static final int ERROR = 40;
    public static final String LOG_FILE_NAME_PATTERN = "yyyy-MM-dd'T'HH_mm_ss";
    public static final String LOG_FILE_TYPE = ".log";
    public static final String SQLITE_FILE_TYPE = ".db";
    /**
     *  Ansi格式的带颜色的控制台字符控制
     */
    public static final MemorySegment ANSI_PREFIX = MemorySegment.ofArray("\033[".getBytes(StandardCharsets.UTF_8));
    public static final MemorySegment ANSI_SUFFIX = MemorySegment.ofArray("\033[0m".getBytes(StandardCharsets.UTF_8)); // 4 bytes
    public static final MemorySegment TRACE_BYTES = MemorySegment.ofArray("TRACE".getBytes(StandardCharsets.UTF_8));
    public static final MemorySegment DEBUG_BYTES = MemorySegment.ofArray("DEBUG".getBytes(StandardCharsets.UTF_8));
    public static final MemorySegment INFO_BYTES = MemorySegment.ofArray("INFO".getBytes(StandardCharsets.UTF_8));
    public static final MemorySegment WARN_BYTES = MemorySegment.ofArray("WARN".getBytes(StandardCharsets.UTF_8));
    public static final MemorySegment ERROR_BYTES = MemorySegment.ofArray("ERROR".getBytes(StandardCharsets.UTF_8));
    public static final MemorySegment RED_BYTES = MemorySegment.ofArray("31m".getBytes(StandardCharsets.UTF_8));
    public static final MemorySegment GREEN_BYTES = MemorySegment.ofArray("32m".getBytes(StandardCharsets.UTF_8));
    public static final MemorySegment YELLOW_BYTES = MemorySegment.ofArray("33m".getBytes(StandardCharsets.UTF_8));
    public static final MemorySegment BLUE_BYTES = MemorySegment.ofArray("34m".getBytes(StandardCharsets.UTF_8));
    public static final MemorySegment MAGENTA_BYTES = MemorySegment.ofArray("35m".getBytes(StandardCharsets.UTF_8));
    public static final MemorySegment CYAN_BYTES = MemorySegment.ofArray("36m".getBytes(StandardCharsets.UTF_8));
    public static final String RED = "red";
    public static final String GREEN = "green";
    public static final String YELLOW = "yellow";
    public static final String BLUE = "blue";
    public static final String MAGENTA = "magenta";
    public static final String CYAN = "cyan";
    /**
     * 文件分隔符在windows linux macOS中都应该使用'/'
     */
    public static final String SEPARATOR = "/";
    /**
     * log配置文件名称
     */
    public static final String DEFAULT_LOG_CONFIG_NAME = "/log.json";
    public static final int DEFAULT_LOG_QUEUE_SIZE = NativeUtil.getCpuCores() << 2;
    public static final int DEFAULT_STRING_BUILDER_SIZE = 128;
    public static final int KB = 1024;
    public static final int MB = 1024 * KB;
    public static final int GB = 1024 * MB;
    public static final int SECOND = 1000;
    public static final int MINUTE = 60 * SECOND;
    public static final int HOUR = 60 * MINUTE;
    public static final int DAY = 24 * HOUR;
    public static final int PAGE_SIZE = 4 * KB;
    public static final String UNREACHED = "Should never be reached";

    public static final Object[] EMPTY_ARRAY = {};
    public static final String EMPTY_STRING = "";
    public static final String NULL_STRING = "null";
    public static final String L_BRACKET = "(";
    public static final String R_BRACKET = ")";
    public static final String LINE_SEPARATOR = System.lineSeparator();

    public static final String ID = "id";
    public static final byte[] EMPTY_BYTES = new byte[0];
    public static final String MINT = "mint";
    public static final String GATEWAY = "gateway";
    public static final String SHA_256 = "SHA-256";
    public static final String MD_5 = "MD5";

    public static final String HEAP = "heap";
    public static final String NON_HEAP = "non-heap";

    public static final String RPC_ENCODER = "rpc-encoder";
    public static final String RPC_DECODER = "rpc-decoder";
    public static final String RPC_CLIENT_HANDLER = "rpc-client-handler";
    public static final String RPC_SERVER_HANDLER = "rpc-server-handler";
    public static final String RPC_CLIENT_CHANNEL_LIMIT = "rpc-client-traffic-limit";
    public static final String RPC_SERVER_CHANNEL_LIMIT = "rpc-server-traffic-limit";
    public static final String HEARTBEAT_HANDLER = "heartbeat-handler";
    public static final String FILE_CLIENT_HANDLER = "file-server-handler";
    public static final String FILE_SERVER_HANDLER = "file-server-handler";
    public static final String HTTP_SSL = "http-ssl";
    public static final String HTTP_CODEC = "http-codec";
    public static final String HTTP_COMPRESSOR = "http-compressor";
    public static final String HTTP_DECOMPRESSOR = "http-decompressor";
    public static final String HTTP_AGGREGATOR = "http-aggregator";
    public static final String HTTP_CHUNKED = "http-chunked";
    public static final String HTTP_HANDLER = "http-handler";
    public static final String PG_CONNECTION_HANDLER = "pg-conn-handler";
    public static final String PG_DECODER = "pg-decoder";
    public static final String PG_MSG_HANDLER = "pg-rpcMsg-handler";

    public static final String PG_SSL = "pg-ssl";


    public static final Integer DEFAULT_RETRY_COUNT = 3;

    public static final Integer DEFAULT_GATEWAY_APP_ID = 1010;
    public static final Integer DEFAULT_MINT_APP_ID = 1020;

    /**
     *   json
     */
    public static final String JSON_KEY_TYPE_ERR = "Unsupported json key type";
    public static final String JSON_VALUE_TYPE_ERR = "Unsupported json value type";

    /**
     * cache
     */
    public static final String PATTERN = "[A-Za-z0-9]+";
    public static final String RACE = "race detected";
    public static final String DEFAULT = "default";
    public static final String LOCAL_DB_DIR = "db";
    public static final String CLUSTER_DB_DIR = "cluster_db";
    public static final String CLUSTER_META_DIR = "cluster_meta";
    public static final String CLUSTER_BACK_UP_DIR = "cluster_backup";
    public static final String KV_CACHE_PREFIX = "kv_";
    public static final String LIST_CACHE_PREFIX = "list_";
    public static final String SET_CACHE_PREFIX = "set_";
    public static final String PRIORITY_SET_CACHE_PREFIX = "priority_set_";
    public static final byte[] LIST_START_BYTES = "start".getBytes(StandardCharsets.UTF_8);
    public static final byte[] LIST_END_BYTES = "end".getBytes(StandardCharsets.UTF_8);
    public static final byte[] SET_COUNT_BYTES = "count".getBytes(StandardCharsets.UTF_8);
    public static final byte[] RAFT_TERM = "raft_term".getBytes(StandardCharsets.UTF_8);
    public static final byte[] RAFT_VOTED_FOR = "raft_voted_for".getBytes(StandardCharsets.UTF_8);
    public static final byte[] RAFT_LOG_FIRST_INDEX = "raft_log_first_index".getBytes(StandardCharsets.UTF_8);
    public static final byte[] RAFT_LOG_LAST_INDEX = "raft_log_last_index".getBytes(StandardCharsets.UTF_8);
    public static final byte[] RAFT_APPLY_INDEX = "raft_apply_index".getBytes(StandardCharsets.UTF_8);
    public static final String FAVICON_ICO = "/favicon.ico";
    public static final String JSON_SUFFIX = ".json";
    public static final String WEBSOCKET_URL_PREFIX = "ws://localhost:";
    public static final String JSON_HEADER = "application/json; charset=UTF-8";
    public static final String SERVICE_NOT_FOUND = "Target service not found";
    public static final String WRONG_PATH = "Uri path should start with '/' ";
    public static final String WRONG_APP = "Unable to find target app, please check your uri...";
    public static final String APP_NOT_ONLINE = "App is not online";
    public static final String MINT_NOT_FOUND = "Currently not connected to mint";
    public static final String UNSUPPORTED_MSG_TYPE = "Unsupported rpcMsg type";
    public static final String UNSUPPORTED = "Unsupported operation";
    public static final String UNSUPPORTED_CHANNEL_TYPE = "Unsupported channel type";
    public static final String MISSING_HEADER = "Missing header content";
    public static final String MISSING_PARAM = "Missing param content";
    public static final String NOT_FOUND = "Not Found";
    public static final String MISSING_PATH_VARIABLE = "Missing path variable content";
    public static final String ILLEGAL_ACCESS = "Illegal access";
    public static final String NOT_LOGIN = "Current user not login";
    public static final String ALREADY_EXIST = "User already exist";
    public static final String DB_CONNECTION_ERR = "Can't send message to postgresql";
    public static final String AUTHORIZATION = "Authorization";
    public static final String BEARER = "Bearer ";
    public static final Integer BEARER_SIZE = BEARER.length();
    public static final String DATE_FORMAT = "yyyy-MM-dd HH:mm:ss";
    public static final String NOT_ROOT_USER = "not root user";
    public static final String WEB_SOCKET = "websocket";
    public static final String UPGRADE = "Upgrade";
    public static final String OK = "200";
    public static final String OK_STR = "OK";
    public static final String ERR = "500";
    public static final String SUCCESS = "success";
    public static final String FAIL = "fail";
    public static final String REACHING_RATE_LIMIT = "reaching rate limit, please wait a while";
    /**
     * thread factory name
     */
    public static final String RPC_CLIENT_WORKER_THREAD = "rpc-c-w";
    public static final String RPC_SERVER_IO_THREAD = "rpc-s-io";
    public static final String RPC_SERVER_WORKER_THREAD = "rpc-s-w";
    public static final String VIRTUAL_THREAD = "vir";
    public static final String PLATFORM_THREAD = "plat";
    public static final String SHUT_DOWN = "shutdown";
    public static final String EVENT_SOURCE = "event-source";
    public static final String RAFT_CORE_THREAD = "raft-core"; // raft核心线程,固定为1个
    public static final String RAFT_JOB_THREAD = "raft-job"; // raft定时任务线程,固定为1个
    public static final String WHEEL_CORE = "wheel-core";
    public static final String WHEEL_WAIT = "wheel-wait";
    public static final String CHANNEL_EVENT = "channel-event";
    public static final String MINT_DISCOVERY = "mint-discovery";
    public static final String APP_DISCOVERY = "app-discovery";
    public static final String GATEWAY_DISCOVERY = "gateway-discovery";
    /**
     * job related
     */
    // server对client连接发起认证,超时关闭连接的延时任务
    public static final String RPC_SERVER_AUTH_JOB = "rpc_server_auth-";
    // app定时拉取信息与重定向mint任务
    public static final String APP_FETCH_FROM_MINT = "app_fetch_mint";
    // gateway定时拉取信息与重定向mint任务
    public static final String GATEWAY_FETCH_FROM_MINT = "gateway_fetch_mint";
    // 分布式锁等待超时的延时任务
    public static final String MINT_LOCK_TIMEOUT = "mint_lock_timeout-";
    // leader定期向所有follower发送心跳
    public static final String SEND_HEARTBEAT = "leader_to_follower_heartbeat";
    // follower检测leader心跳是否超时
    public static final String RECEIVE_HEARTBEAT = "follower_receive_heartbeat";
    // pre candidate选举任务
    public static final String PRE_CANDIDATE_ELECTION = "pre_candidate_election";
    // pre candidate选举任务到期
    public static final String PRE_CANDIDATE_ELECTION_TIMEOUT = "pre_candidate_election_timeout";
    // candidate选举任务
    public static final String CANDIDATE_ELECTION = "candidate_election";
    // candidate选举任务到期
    public static final String CANDIDATE_ELECTION_TIMEOUT = "candidate_election_timeout";
    // 文件发送超时
    public static final String FILE_SEND_TIMEOUT = "file_send_timeout";
    // 文件接收超时
    public static final String FILE_RECEIVE_TIMEOUT = "file_receive_timeout";

    /**
     * sql
     */
    public static final BigDecimal HUNDRED = BigDecimal.valueOf(100L);
    public static final char SIGN_CHAR = '$';
    public static final char COMMA_CHAR = ',';
    public static final int DEFAULT_BATCH = 256;
    public static final String WHERE = "#where{}";
    public static final int WHERE_LENGTH = 8;
    public static final String DYNAMIC_SQL_ERR = "Err occurred in dynamic sql";

    /**
     *  epoll
     */
    public static final int EPOLL_IN = 1;
    public static final int EPOLL_OUT = 1 << 2;
    public static final int EPOLL_ERR = 1 << 3;
    public static final int EPOLL_HUP = 1 << 4;
    public static final int EPOLL_RDHUP = 1 << 13;

    public static final int EPOLL_CTL_ADD = 1;
    public static final int EPOLL_CTL_DEL = 2;
    public static final int EPOLL_CTL_MOD = 3;

    /**
     *   kqueue
     */
    public static final short EVFILT_READ = -1;
    public static final short EVFILT_WRITE = -2;
    public static final short EV_ADD = 1;
    public static final short EV_DELETE = 1 << 1;

    /**
     *   Net
     */
    public static final int NET_NONE = 1;
    public static final int NET_IGNORED = 1 << 1;
    public static final int NET_UPDATE = 1 << 2;
    public static final int NET_W = NET_NONE | 1 << 3; // register write only
    public static final int NET_PW = 1 << 4; // register write if possible
    public static final int NET_R = NET_NONE | 1 << 5; // register read only
    public static final int NET_PR = 1 << 6; // register read if possible
    public static final int NET_RW = NET_NONE | NET_R | NET_W; // register read and write
    public static final int NET_PRW = NET_PR | NET_PW; // register read and write if possible
    public static final int NET_PC = 1 << 7;
    public static final int NET_WC = 1 << 8;
    public static final int NET_OTHER = 1 << 9;

    /**
     *   Ssl library
     */
    public static final int SSL_FILETYPE_PEM = 1;
    public static final int SSL_ERROR_SSL = 1;
    public static final int SSL_ERROR_WANT_READ = 2;
    public static final int SSL_ERROR_WANT_WRITE = 3;
    public static final int SSL_ERROR_SYSCALL = 5;
    public static final int SSL_ERROR_ZERO_RETURN = 6;
    public static final long SSL_MODE_ENABLE_PARTIAL_WRITE = 1L;
    public static final long SSL_MODE_ACCEPT_MOVING_WRITE_BUFFER = 2L;
    public static final long SSL_MODE_AUTO_RETRY = 4L;
    public static final int SSL_VERIFY_NONE = 0;
    public static final int SSL_VERIFY_PEER = 1;
    public static final int SSL_VERIFY_FAIL_IF_NO_PEER_CERT = 2;


    /**
     *   sqlite
     */
    public static final int SQLITE_OK = 0;
    public static final int SQLITE_ERR = 1;
    public static final int SQLITE_OPEN_READONLY = 1;
    public static final int SQLITE_OPEN_READWRITE = 2;
    public static final int SQLITE_OPEN_CREATE = 4;
    public static final int SQLITE_OPEN_NOMUTEX = 32768;
    public static final int SQLITE_OPEN_FULLMUTEX = 65535;
    public static final int SQLITE_OPEN_SHAREDCACHE = 131072;
    public static final int SQLITE_OPEN_PRIVATECACHE = 262144;
    public static final int SQLITE_OPEN_NOFOLLOW = 16777216;
    public static final int SQLITE_CONFIG_SINGLETHREAD = 1;
    public static final int SQLITE_CONFIG_MULTITHREAD = 2;
    public static final int SQLITE_CONFIG_SERIALIZED = 3;

    public static final int SQLITE_INTEGER = 1;
    public static final int SQLITE_FLOAT = 2;
    public static final int SQLITE_TEXT = 3;
    public static final int SQLITE_BLOB = 4;
    public static final int SQLITE_NULL = 5;

    public static final int SQLITE_ROW = 100;
    public static final int SQLITE_DONE = 101;
    public static final int SQLITE_PREPARE_PERSISTENT = 1;
    public static final int SQLITE_PREPARE_NORMALIZE = 2;
    public static final int SQLITE_PREPARE_NO_VTAB = 4;
    public static final byte SQLITE_UTF8 = 1;

    /**
     *   Postgresql
     */
    public static final byte PG_SSL_OK = 'S';
    public static final byte PG_SSL_DISABLE = 'N';
    public static final String PG_SSL_PREFER = "prefer";
    public static final String PG_SSL_VERIFY_CA = "verify-ca";
    public static final String PG_SSL_VERIFY_FULL = "verify-full";
    public static final byte PG_AUTH = 'R';
    public static final byte PG_PASSWORD = 'p';
    public static final byte PG_ERROR = 'E';
    public static final byte PG_PARAMETER_STATUS = 'S';
    public static final byte PG_BACKEND_KEY_DATA = 'K';
    public static final byte PG_READY = 'Z';
    public static final byte PG_TRANSACTION_IDLE = 'I';
    public static final byte PG_TRANSACTION_ON = 'T';
    public static final byte PG_TRANSACTION_ERROR = 'E';
    public static final byte PG_SIMPLE_QUERY = 'Q';
    public static final byte PG_COMMAND_COMPLETION = 'C';
    public static final int PG_AUTH_OK = 0;
    public static final int PG_AUTH_CLEAR_TEXT_PASSWORD = 3;
    public static final int PG_AUTH_MD5_PASSWORD = 5;
    public static final int PG_AUTH_SASL_PASSWORD = 10;
    public static final int PG_AUTH_SASL_CONTINUE = 11;
    public static final int PG_AUTH_SASL_FINAL = 12;


    private Constants() {
        throw new FrameworkException(ExceptionType.CONTEXT, Constants.UNREACHED);
    }
}
