package cc.zorcc.core;

/**
 *   Line represents a single line from the target source file
 */
public record Line(
        String value,
        int indent
) {
}
