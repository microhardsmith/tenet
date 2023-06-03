package cn.zorcc.orm.pg;

public record PgNoDataMsg() {
    public static final PgNoDataMsg INSTANCE = new PgNoDataMsg();
}
