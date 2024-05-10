package cn.zorcc.common.serde;

import cn.zorcc.common.Constants;
import cn.zorcc.common.ExceptionType;
import cn.zorcc.common.exception.FrameworkException;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

/**
 *   Code block represents a chunk of code-lines, designed to help control over the indent issue
 */
public final class CodeBlock {

    public static CodeBlock IGNORED = new CodeBlock();

    private List<Content> contents;
    private int currentIndent;

    public CodeBlock addLine(String content) {
        if(contents == null) {
            contents = new ArrayList<>();
        }
        contents.add(new Content(content, currentIndent));
        return this;
    }

    public CodeBlock addBlock(CodeBlock c) {
        if(c == IGNORED) {
            return this;
        }
        if(contents == null) {
            contents = new ArrayList<>();
        }
        for (Content ct : c.contents) {
            contents.add(new Content(ct.value(), currentIndent + ct.indent()));
        }
        return this;
    }

    public CodeBlock addBlocks(Collection<CodeBlock> cs) {
        for (CodeBlock c : cs) {
            CodeBlock _ = addBlock(c);
        }
        return this;
    }

    public CodeBlock newLine() {
        if(contents == null) {
            contents = new ArrayList<>();
        }
        contents.add(new Content("", currentIndent));
        return this;
    }

    public CodeBlock indent() {
        currentIndent++;
        return this;
    }

    public CodeBlock unindent() {
        if(--currentIndent < 0) {
            throw new FrameworkException(ExceptionType.ANNO, Constants.UNREACHED);
        }
        return this;
    }

    public List<Content> contents() {
        return List.copyOf(contents);
    }
}
