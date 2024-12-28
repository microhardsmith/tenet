package cc.zorcc.tenet.serdeproc;

import cc.zorcc.tenet.serde.SerdeException;
import cc.zorcc.tenet.serde.Transform;
import cc.zorcc.tenet.serde.Transformer;
import cc.zorcc.tenet.serde.TransformData;

import javax.annotation.processing.AbstractProcessor;
import javax.annotation.processing.RoundEnvironment;
import javax.lang.model.SourceVersion;
import javax.lang.model.element.Element;
import javax.lang.model.element.TypeElement;
import javax.lang.model.type.DeclaredType;
import javax.lang.model.type.TypeMirror;
import javax.lang.model.util.Elements;
import javax.lang.model.util.Types;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;

/**
 *   Transform processor for handling all the classes annotated with @Transform
 */
public final class TransformProcessor extends AbstractProcessor {
    private final List<String> generatedClasses = new ArrayList<>();

    @Override
    public SourceVersion getSupportedSourceVersion() {
        return SourceVersion.latestSupported();
    }

    @Override
    public Set<String> getSupportedAnnotationTypes() {
        return Set.of(Transform.class.getCanonicalName());
    }

    @Override
    public boolean process(Set<? extends TypeElement> annotations, RoundEnvironment roundEnv) {
        if(roundEnv.processingOver()) {
            writeTransformHintToResources();
        }else {
            doProcessing(roundEnv);
        }
        return true;
    }

    private void writeTransformHintToResources() {
        System.out.println("Writing transform hint to resources");
    }

    private void doProcessing(RoundEnvironment roundEnv) {
        Set<? extends Element> elements = roundEnv.getElementsAnnotatedWith(Transform.class);

        for (Element element : elements) {

            return ; // TODO
        }
    }

    private TransformData getTransformerInfo(Element element) {
        Types typeUtils = processingEnv.getTypeUtils();
        Elements elementUtils = processingEnv.getElementUtils();
        TypeMirror targetTypeMirror = elementUtils.getTypeElement(Transformer.class.getName()).asType();
        if (element instanceof TypeElement typeElement) {
            List<? extends TypeMirror> interfaces = typeElement.getInterfaces();
            for (TypeMirror typeMirror : interfaces) {
                if (typeUtils.isSameType(typeMirror, targetTypeMirror) && typeMirror instanceof DeclaredType declaredType) {
                    List<? extends TypeMirror> args = declaredType.getTypeArguments();
                    if(args.size() == 2) {
                        TypeMirror aTypeMirror = args.getFirst();
                        TypeMirror bTypeMirror = args.getLast();
                        System.out.println("A : %s, B : %s".formatted(aTypeMirror, bTypeMirror));
                    } else {
                        throw new SerdeException("Type arguments mismatch");
                    }
                }
            }
        } else {
            throw new SerdeException("@Transform could only be used on TypeElement");
        }
        // TODO
        return null;
    }
}
