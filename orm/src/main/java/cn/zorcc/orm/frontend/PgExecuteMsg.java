package cn.zorcc.orm.frontend;

/**
 *
 * @param portal The name of the portal to execute (an empty string selects the unnamed portal).
 * @param maxRowsToReturn Maximum number of rows to return, if portal contains a query that returns rows (ignored otherwise). Zero denotes “no limit”.
 */
public record PgExecuteMsg(
    String portal,
    int maxRowsToReturn
) {
}
