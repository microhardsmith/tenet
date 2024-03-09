package cn.zorcc.common.structure;

import org.junit.jupiter.api.Test;

public class IntHolderTest {
    @Test
    public void testIntHolderLock() {
        IntHolder holder = new IntHolder(0);
        holder.transform(i -> i + 1, Thread::onSpinWait);
        System.out.println(holder.getVolatileValue());
    }
}
