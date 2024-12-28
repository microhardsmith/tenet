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
    private static final String INDENT = " ".repeat(4);
    private final Set<String> imports = new HashSet<>();
    private final String packageName;
    private final String className;
    private final Deque<Block> blocks = new ArrayDeque<>();

    public Source(String packageName, String className) {
        this.packageName = packageName;
        this.className = className;
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

    public void registerImports(Class<?>... clazzList) {
        for (Class<?> c : clazzList) {
            imports.add(c.getName());
        }
    }

    public void registerPackageImports(String packageName) {
        imports.add(packageName + ".*");
    }

    /**
     *   Writing all the source code to target filer
     */
    public void writeToFiler(Filer filer) {
        try{
            JavaFileObject sourceFile = filer.createSourceFile("%s.%s".formatted(packageName, className));
            try(PrintWriter out = new PrintWriter(sourceFile.openWriter())) {
                out.println("package %s;\n".formatted(packageName));
                for(String s : imports) {
                    out.println("import %s;".formatted(s));
                }
                out.println();
                for(Block block : blocks) {
                    for (Line line : block.lines()) {
                        out.print(INDENT.repeat(line.indent()));
                        out.println(line.value());
                    }
                    out.println();
                }
            } catch (IOException e) {
                throw new SerdeException("Can't write to target source file", e);
            }
        } catch (IOException e) {
            throw new SerdeException("Can't create target source file", e);
        }
    }
}
