package cn.zorcc.orm.backend;

public record PgCloseCompleteMsg() {
    public static final PgCloseCompleteMsg INSTANCE = new PgCloseCompleteMsg();
}
