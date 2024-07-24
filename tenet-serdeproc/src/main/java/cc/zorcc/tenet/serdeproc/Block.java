package cc.zorcc.tenet.serdeproc;

import cc.zorcc.tenet.serde.SerdeException;

import java.util.ArrayList;
import java.util.List;

/**
 *   Block represents a chunk of code-lines, designed to help control over the indent issue
 *   Block is not threadSafe, considering it as a StringBuilder designer for source code generation
 */
public final class Block {
    /**
     * A global instance representing an ignored block.
     */
    public static Block IGNORED = new Block();

    private final List<Line> lines = new ArrayList<>();
    private int currentIndent;

    /**
     * Adds a new line to the current Block with the specified content.
     * The line will have the current indentation level.
     *
     * @param content the content of the line to be added.
     * @return the current Block instance for method chaining.
     */
    public Block addLine(String content) {
        lines.add(new Line(content, currentIndent));
        return this;
    }

    /**
     * Adds an empty line to the current Block for better visualization.
     * The empty line will have the current indentation level.
     *
     * @return the current Block instance for method chaining.
     */
    public Block newLine() {
        lines.add(new Line("", currentIndent));
        return this;
    }

    /**
     * Increases the current indentation level by one.
     *
     * @return the current Block instance for method chaining.
     */
    public Block indent() {
        currentIndent++;
        return this;
    }

    /**
     * Decreases the current indentation level by one.
     * Throws a SerdeException if the resulting indentation level is negative.
     *
     * @return the current Block instance for method chaining.
     * @throws SerdeException if the indentation level becomes negative.
     */
    public Block unindent() {
        if(--currentIndent < 0) {
            throw new SerdeException("Indent could be negative");
        }
        return this;
    }

    /**
     * Returns an unmodifiable copy of the lines in the current Block.
     *
     * @return an unmodifiable List of lines.
     */
    public List<Line> lines() {
        return List.copyOf(lines);
    }
}
