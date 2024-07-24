package cc.zorcc.tenet.serdeproc;

/**
 * The Line record represents a single line from the target source file.
 * It encapsulates the content of the line and its indentation level.
 * @param value the content of the line
 * @param indent the indentation level of the line
 */
public record Line(
        String value,
        int indent
) {
}
