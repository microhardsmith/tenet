package cn.zorcc.orm.common;

import cn.zorcc.common.enums.ExceptionType;
import cn.zorcc.common.exception.FrameworkException;
import cn.zorcc.common.util.ClassUtil;
import cn.zorcc.orm.anno.Col;
import cn.zorcc.orm.anno.Del;
import cn.zorcc.orm.anno.Id;
import cn.zorcc.orm.anno.Table;

import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

public record TableDescription(
    String tableName,
    List<String> cols,
    String allCol,
    boolean preFill,
    boolean generatingId,
    String delField,
    Object delArg,
    Object notDelArg
) {
    record Rank(String str, int ordinal) implements Comparator<Rank> {
        @Override
        public int compare(Rank o1, Rank o2) {
            return Integer.compare(o1.ordinal, o2.ordinal);
        }
    }
    public static TableDescription of(Class<?> clazz) {
        if(!clazz.isAnnotationPresent(Table.class)) {
            throw new FrameworkException(ExceptionType.SQL, "No @Table() found on po class : %s".formatted(clazz.getSimpleName()));
        }
        String tableName = clazz.getAnnotation(Table.class).name();
        boolean genId = false;
        String delField = null;
        Object delArg = null;
        Object notDelArg = null;
        List<Field> allFields = ClassUtil.getAllFields(clazz);
        List<Rank> ranks = new ArrayList<>(allFields.size());
        for (Field f : allFields) {
            if(f.isAnnotationPresent(Id.class)) {
                Id idAnno = f.getAnnotation(Id.class);
                genId = idAnno.auto();
                ranks.add(new Rank(idAnno.value(), idAnno.ordinal()));
            }else if(f.isAnnotationPresent(Del.class)) {
                Del delAnno = f.getAnnotation(Del.class);
                delField = f.getName();
                Class<?> type = f.getType();
                if(type == Integer.class) {
                    delArg = 1;
                    notDelArg = 0;
                }else if(type == Short.class) {
                    delArg = (short) 1;
                    notDelArg = (short) 0;
                }else if(type == Long.class) {
                    delArg = 1L;
                    notDelArg = 0L;
                }else if(type == Boolean.class) {
                    delArg = Boolean.TRUE;
                    notDelArg = Boolean.FALSE;
                }else {
                    throw new FrameworkException(ExceptionType.SQL, "Unrecognized delField");
                }
                ranks.add(new Rank(delAnno.value(), delAnno.ordinal()));
            }else if(f.isAnnotationPresent(Col.class)) {
                Col colAnno = f.getAnnotation(Col.class);
                ranks.add(new Rank(colAnno.value(), colAnno.ordinal()));
            }
        }
        boolean preFill = Filler.class.isAssignableFrom(clazz);
        List<String> cols = ranks.stream().sorted().map(Rank::str).toList();
        String allCol = String.join(",", cols);
        return new TableDescription(tableName, cols, allCol, preFill, genId, delField, delArg, notDelArg);
    }
}
