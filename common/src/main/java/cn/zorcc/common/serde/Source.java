package cn.zorcc.common.serde;

import cn.zorcc.common.Constants;
import cn.zorcc.common.ExceptionType;
import cn.zorcc.common.exception.FrameworkException;

import javax.annotation.processing.Filer;
import javax.tools.JavaFileObject;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.ArrayDeque;
import java.util.Deque;
import java.util.HashSet;
import java.util.Set;

/**
 *   Source representing a java source file which could be written into a Filer during complication
 *   No type checking, No complicated mechanism, just simple string concatenation, it's developer's duty to make it all work
 */
public final class Source {
    private final Set<String> imports = new HashSet<>();
    private final String packageName;
    private final String className;
    private final Set<String> implNames;
    private final Deque<CodeBlock> blocks = new ArrayDeque<>();
    private int indent = 0;

    public Source(String packageName, String className, Set<String> implNames) {
        this.packageName = packageName;
        this.className = className;
        this.implNames = implNames;
    }

    public String name() {
        return className;
    }

    public void registerBlock(CodeBlock block) {
        if(block != CodeBlock.IGNORED) {
            blocks.addLast(block);
        }
    }

    /**
     *   Register import class
     */
    public void registerImports(Class<?>... classes) {
        for (Class<?> c : classes) {
            imports.add(c.getName());
        }
    }

    private void checkStructure() {
        if(packageName == null || packageName.isBlank() || className == null || className.isBlank()) {
            throw new FrameworkException(ExceptionType.ANNO, Constants.UNREACHED);
        }
    }

    private void write(PrintWriter out, String content) {
        out.println("\t".repeat(indent) + content);
    }

    private void write(PrintWriter out, CodeBlock block) {
        for (Content b : block.contents()) {
            out.print("\t".repeat(b.indent() + indent));
            out.println(b.value());
        }
        out.println();
    }

    private void newLine(PrintWriter out) {
        out.println();
    }

    private Source writePackage(PrintWriter out) {
        write(out, "package %s;".formatted(packageName));
        newLine(out);
        return this;
    }

    private Source writeImports(PrintWriter out) {
        for(String s : imports) {
            write(out, "import %s;".formatted(s));
        }
        newLine(out);
        return this;
    }

    private Source writeDeclaration(PrintWriter out) {
        if(implNames == null || implNames.isEmpty()) {
            write(out, "public final class %s {".formatted(className));
        }else {
            write(out, "public final class %s implements %s {".formatted(className, String.join(",", implNames)));
        }
        indent++;
        newLine(out);
        return this;
    }

    private Source writeBlocks(PrintWriter out) {
        for(CodeBlock block : blocks) {
            write(out, block);
            newLine(out);
        }
        return this;
    }

    private void writeClosure(PrintWriter out) {
        indent--;
        newLine(out);
        write(out, "}");
        newLine(out);
        out.flush();
    }

    public void writeToFiler(Filer filer) {
        checkStructure();
        try{
            JavaFileObject sourceFile = filer.createSourceFile(className);
            try (PrintWriter out = new PrintWriter(sourceFile.openWriter())) {
                this.writePackage(out)
                        .writeImports(out)
                        .writeDeclaration(out)
                        .writeBlocks(out)
                        .writeClosure(out);
            }
        } catch (IOException e) {
            throw new FrameworkException(ExceptionType.ANNO, "Couldn't create source file", e);
        }

    }
}
