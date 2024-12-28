package cc.zorcc.tenet.serdeproc;

import org.jspecify.annotations.NonNull;

import javax.lang.model.element.VariableElement;
import java.util.List;
import java.util.Map;

public record FieldData(
        @NonNull VariableElement field,
        @NonNull String fieldName,
        @NonNull String fieldType,
        @NonNull String fieldClass,
        @NonNull List<Map.Entry<String, String>> attrList
) {
}
