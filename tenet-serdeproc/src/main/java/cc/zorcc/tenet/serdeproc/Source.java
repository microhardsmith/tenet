package cc.zorcc.tenet.serdeproc;

import cc.zorcc.tenet.serde.SerdeException;

import javax.annotation.processing.Filer;
import javax.tools.JavaFileObject;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.*;

/**
 *   Source representing a java source file which could be written into a Filer during complication
 *   No type checking, No complicated mechanism, just simple string concatenation, it's developer's duty to make it all work
 */
public final class Source {
    private final Set<String> imports = new HashSet<>();
    private final String packageName;
    private final String className;
    private final Set<String> implNames;
    private final Deque<Block> blocks = new ArrayDeque<>();
    private int indent = 0;

    public Source(String packageName, String className, Set<String> implNames) {
        this.packageName = packageName;
        this.className = className;
        this.implNames = implNames;
    }

    public String name() {
        return className;
    }

    public void registerBlock(Block block) {
        if(block != Block.IGNORED) {
            blocks.addLast(block);
        }
    }

    public void registerBlocks(List<Block> blocks) {
        for (Block block : blocks) {
            registerBlock(block);
        }
    }

    /**
     *   Register class import
     */
    public void registerImport(Class<?> clazz) {
        imports.add(clazz.getName());
    }

    /**
     *   Register class imports
     */
    public void registerImports(List<Class<?>> clazzList) {
        for (Class<?> c : clazzList) {
            registerImport(c);
        }
    }

    private void write(PrintWriter out, String content) {
        out.println("\t".repeat(indent) + content);
    }

    private void write(PrintWriter out, Block block) {
        for (Line line : block.lines()) {
            out.print("\t".repeat(line.indent() + indent));
            out.println(line.value());
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
        for(Block block : blocks) {
            write(out, block);
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

    /**
     *   Writing all the source code to target filer
     */
    public void writeToFiler(Filer filer) {
        if(packageName == null || packageName.isBlank() || className == null || className.isBlank()) {
            throw new SerdeException("Source corrupted");
        }
        try{
            JavaFileObject sourceFile = filer.createSourceFile("%s.%s".formatted(packageName, className));
            try(PrintWriter out = new PrintWriter(sourceFile.openWriter())) {
                writePackage(out)
                    .writeImports(out)
                    .writeDeclaration(out)
                    .writeBlocks(out)
                    .writeClosure(out);
            } catch (IOException e) {
                throw new SerdeException("Can't write to target source file", e);
            }
        } catch (IOException e) {
            throw new SerdeException("Can't create target source file", e);
        }
    }
}
