package cc.zorcc.tenet.serdeproc;

import javax.lang.model.element.Element;
import java.util.List;

public record SerdeData(
        Element element,
        String packageName,
        String targetClassName,
        String generatedClassName,
        GenericData genericData,
        List<FieldData> fieldDataList,
        List<FieldData> enumConstantList
) {
}
