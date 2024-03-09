package cn.zorcc.common.structure;

import cn.zorcc.common.network.ListenerTask;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.util.List;

public class TaskQueueTest {
    @Test
    public void testTaskQueue() {
        TaskQueue<ListenerTask> queue = new TaskQueue<>(10);
        for(int i = 0; i < 100; i++) {
            queue.offer(new ListenerTask(null, null, null, null, null, null, null));
        }
        List<ListenerTask> elements = queue.elements();
        Assertions.assertEquals(elements.size(), 100);
    }
}
