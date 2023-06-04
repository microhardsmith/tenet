package cn.zorcc.orm.pg;

public record PgFlushMsg() {
    public static final PgFlushMsg INSTANCE = new PgFlushMsg();
}
