package cn.zorcc.common.postgre;

/**
 *  Corresponding to RowDescription in the documentation <a href="https://www.postgresql.org/docs/current/protocol-message-formats.html">postgresql message format</a>
 */
public record PgRowDescription(
        String fieldName,
        int tableOid,
        short attr,
        int fieldOid,
        short typeSize,
        int modifier,
        short format
) {

}
