package cn.zorcc.common.serde;

/**
 *   Represents a line of source code with indent
 */
public record Content(
        String value,
        int indent
) {

}
