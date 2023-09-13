package cn.zorcc.common.context;

import cn.zorcc.common.Record;
import cn.zorcc.common.bean.Alpha;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

public class RecordTest {
    @Test
    public void testRecord() {
        Record<Alpha> rRecord = Record.of(Alpha.class);
        Object[] args = new Object[rRecord.elementSize()];
        Integer i = 10;
        String s = "hello";
        rRecord.assign(args, "a", i);
        rRecord.assign(args, "b", s);
        Alpha alpha = rRecord.construct(args);
        Assertions.assertEquals(alpha.a(), i);
        Assertions.assertEquals(alpha.b(), s);
    }
}
