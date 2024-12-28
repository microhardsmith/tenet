package cc.zorcc.tenet.serdeproc;

import java.util.List;

public record GenericData(
        List<String> genericTypes,
        String fullGenericType,
        String simpleGenericType,
        String holderGenericType,
        String emptyGenericType
) {
    public static final GenericData EMPTY = new GenericData(List.of(), "", "", "", "");
}
