package cn.zorcc.common.postgre;

/**
 *   Postgresql transaction status code
 */
public enum PgStatus {
    UNSET,
    IDLE,
    TRANSACTION_ON,
    TRANSACTION_FAIL
}
