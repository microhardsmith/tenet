package cn.zorcc.orm.core;

/**
 *   Postgresql transaction status code
 */
public enum PgStatus {
    IDLE,
    TRANSACTION_ON,
    TRANSACTION_FAIL
}
