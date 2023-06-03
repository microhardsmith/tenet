package cn.zorcc.orm.pg;

/**
 *   Postgresql transaction status code
 */
public enum PgStatus {
    IDLE,
    TRANSACTION_ON,
    TRANSACTION_FAIL
}
