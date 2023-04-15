package cn.zorcc.orm.pg;

import io.netty.buffer.ByteBuf;

/**
 * 存储postgresql使用的一些常量
 */
public class PgConstants {
    public static final String ERR_MSG = "Postgresql state corrupted";
    public static final String SSL_PREFERRED = "preferred";
    public static final String SSL_VERIFY_CA = "verify-ca";
    public static final String SSL_VERIFY_FULL = "verify-full";
    public static final String UNNAMED_COLUMN = "?column?";

    public static final byte TRANSACTION_IDLE = 'I';
    public static final byte TRANSACTION_ON = 'T';
    public static final byte TRANSACTION_ERROR = 'E';

    public static final String NONE = "None";
    public static final char CODE = 'C';
    public static final char MESSAGE = 'M';
    public static final byte AUTH = 'R';
    public static final byte ERR = 'E';
    public static final byte SSL_OK = 'S';
    public static final byte SSL_DISABLE = 'N';
    public static final int AUTH_OK = 0;
    public static final int AUTH_CLEAR_TEXT_PASSWORD = 3;
    public static final int AUTH_MD5_PASSWORD = 5;
    public static final int AUTH_SASL_PASSWORD = 10;
    public static final int AUTH_SASL_CONTINUE = 11;
    public static final int AUTH_SASL_FINAL = 12;
    public static final int SSL_CODE = 80877103;
    public static final byte SASL_RESPONSE = 'p';
    public static final byte PASSWORD = 'p';
    public static final String MD5 = "md5";
    public static final String SCRAM_SHA_256 = "SCRAM-SHA-256";
    public static final String SCRAM_SHA_256_PLUS = "SCRAM-SHA-256-PLUS";
    public static final byte BOOL_TRUE = 't';
    public static final byte BOOL_FALSE = 'f';

    public static final byte BACKEND_KEY_DATA = 'K';
    public static final byte READY = 'Z';
    public static final byte QUERY = 'Q';
    public static final byte DATA_ROW = 'D';
    public static final byte ROW_DESCRIPTION = 'T';
    public static final byte COMMAND_COMPLETE = 'C';
    public static final byte BIND_COMPLETE = '2';
    public static final byte PARSE = 'P';
    public static final byte PARSE_COMPLETE = '1';
    public static final byte DESCRIBE = 'D';
    public static final byte DESCRIBE_STATEMENT = 'S';
    public static final byte DESCRIBE_PORTAL = 'P';
    public static final byte PARAMETER_DESCRIPTION = 't';
    public static final byte SYNC = 'S';
    public static final byte BIND = 'B';
    public static final byte EXECUTE = 'E';
    public static final byte CLOSE = 'C';
    public static final byte NO_DATA = 'n';
    public static final byte PARAMETER_STATUE = 'S';
    public static final byte TERMINATE = 'X';

    public static final String USER = "user";
    public static final String DATABASE = "database";
    public static final String CLIENT_ENCODING = "client_encoding";
    public static final String UTF_8 = "UTF-8";
    public static final String APPLICATION_NAME = "application_name";
    public static final String DATE_STYLE = "DateStyle";
    public static final String ISO = "ISO";
    public static final String SEARCH_PATH = "search_path";
    public static final String FLOAT_PRECISION = "extra_float_digits";
    public static final String DEFAULT_FLOAT_PRECISION = "2"; // float precision control, see https://www.postgresql.org/docs/current/datatype-numeric.html#DATATYPE-FLOAT

    public static final String BEGIN = "begin";
    public static final ByteBuf BEGIN_BYTEBUF = PgUtil.createQueryByteBuf(BEGIN);
    public static final String BEGIN_READ_UNCOMMITTED = "begin transaction isolation level read uncommitted";
    public static final ByteBuf BEGIN_READ_UNCOMMITTED_BYTEBUF = PgUtil.createQueryByteBuf(BEGIN_READ_UNCOMMITTED);
    public static final String BEGIN_READ_COMMITTED = "begin transaction isolation level read committed";
    public static final ByteBuf BEGIN_READ_COMMITTED_BYTEBUF = PgUtil.createQueryByteBuf(BEGIN_READ_COMMITTED);
    public static final String BEGIN_REPEATABLE_READ = "begin transaction isolation level repeatable read";
    public static final ByteBuf BEGIN_REPEATABLE_READ_BYTEBUF = PgUtil.createQueryByteBuf(BEGIN_REPEATABLE_READ);
    public static final String BEGIN_SERIALIZABLE = "begin transaction isolation level serializable";
    public static final ByteBuf BEGIN_SERIALIZABLE_BYTEBUF = PgUtil.createQueryByteBuf(BEGIN_SERIALIZABLE);
    public static final String COMMIT = "commit";
    public static final ByteBuf COMMIT_BYTEBUF = PgUtil.createQueryByteBuf(COMMIT);
    public static final String ROLLBACK = "rollback";
    public static final ByteBuf ROLLBACK_BYTEBUF = PgUtil.createQueryByteBuf(ROLLBACK);


}
