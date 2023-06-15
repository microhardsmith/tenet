package cn.zorcc.orm.frontend;

/**
 *
 * @param type 'S' to describe a prepared statement; or 'P' to describe a portal.
 * @param name The name of the prepared statement or portal to describe (an empty string selects the unnamed prepared statement or portal).
 */
public record PgDescribeMsg(
        byte type,
        String name
) {

}
