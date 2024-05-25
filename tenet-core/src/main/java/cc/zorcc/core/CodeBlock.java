package cc.zorcc.core;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

/**
 *   CodeBlock represents a chunk of code-lines, designed to help control over the indent issue
 *   CodeBlock is not threadSafe, considering it as a StringBuilder designer for source code generation
 */
public final class CodeBlock {
    public static CodeBlock IGNORED = new CodeBlock();

    private final List<Line> lines = new ArrayList<>();
    private int currentIndent;

    /**
     *   Add a new line to current CodeBlock
     */
    public CodeBlock addLine(String content) {
        lines.add(new Line(content, currentIndent));
        return this;
    }

    /**
     *   Merge the target codeBlock into current one
     */
    public CodeBlock addBlock(CodeBlock c) {
        if(c == IGNORED) {
            return this;
        }
        for (Line line : c.lines) {
            lines.add(new Line(line.value(), currentIndent + line.indent()));
        }
        return this;
    }

    /**
     *   Merge multiple codeBlocks into current one
     */
    public CodeBlock addBlocks(Collection<CodeBlock> cs) {
        for (CodeBlock c : cs) {
            CodeBlock _ = addBlock(c);
        }
        return this;
    }

    /**
     *   Creating a new empty line for better visualization
     */
    public CodeBlock newLine() {
        lines.add(new Line("", currentIndent));
        return this;
    }

    /**
     *   Add indent
     */
    public CodeBlock indent() {
        currentIndent++;
        return this;
    }

    /**
     *   Remove indent
     */
    public CodeBlock unindent() {
        if(--currentIndent < 0) {
            throw new FrameworkException(ExceptionType.MISUSE, Constants.UNREACHED);
        }
        return this;
    }

    /**
     *   Create a unmodified copy of current lines
     */
    public List<Line> lines() {
        return List.copyOf(lines);
    }
}
