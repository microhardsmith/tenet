package cn.zorcc.orm.pg;

public record PgSyncMsg() {
    public static final PgSyncMsg INSTANCE = new PgSyncMsg();
}
