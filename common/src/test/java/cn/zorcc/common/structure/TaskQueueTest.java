package cn.zorcc.common.structure;

import cn.zorcc.common.network.ListenerTask;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

public class TaskQueueTest {
    @Test
    public void testTaskQueue() {
        TaskQueue<ListenerTask> queue = new TaskQueue<>(1000);
        for(int i = 0; i < 1000; i++) {
            queue.offer(new ListenerTask(null, null, null, null, null, null, null));
        }
        Iterable<ListenerTask> elements = queue.elements();
        int count = 0;
        while (elements.iterator().hasNext()) {
            ListenerTask listenerTask = elements.iterator().next();
            Assertions.assertNull(listenerTask.provider());
            count++;
        }
        Assertions.assertEquals(count, 1000);
    }
}
