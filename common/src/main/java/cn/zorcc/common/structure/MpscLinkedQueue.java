package cn.zorcc.common.structure;

import cn.zorcc.common.Constants;
import cn.zorcc.common.ExceptionType;
import cn.zorcc.common.exception.FrameworkException;

import java.lang.invoke.MethodHandles;
import java.lang.invoke.VarHandle;

@SuppressWarnings("unused")
public final class MpscLinkedQueue<T> {
    private static final VarHandle producerHandle;
    private static final VarHandle consumerHandle;
    private static final VarHandle nextHandle;

    static {
        try{
            MethodHandles.Lookup lookup = MethodHandles.lookup();
            producerHandle = lookup.findVarHandle(MpscLinkedQueue.class, "producerNode", Node.class);
            consumerHandle = lookup.findVarHandle(MpscLinkedQueue.class, "consumerNode", Node.class);
            nextHandle = lookup.findVarHandle(Node.class, "next", Node.class);
        } catch (ReflectiveOperationException e) {
            throw new FrameworkException(ExceptionType.CONTEXT, Constants.UNREACHED);
        }
    }

    private Node producerNode;
    private Node consumerNode;

    public MpscLinkedQueue() {
        Node node = new Node();
        producerHandle.setRelease(this, node);
        consumerHandle.setRelease(this, node);
    }

    private static class Node {
        private Object current;
        private Node next;

        private Node(Object element) {
            this.current = element;
            nextHandle.setRelease(this, null);
        }

        private Node() {
            this(null);
        }
    }

    public void offer(T element) {
        if(element == null) {
            throw new FrameworkException(ExceptionType.CONTEXT, Constants.UNREACHED);
        }
        Node next = new Node(element);
        Node prev = (Node) producerHandle.getAndSet(this, next);
        nextHandle.setRelease(prev, next);
    }

    @SuppressWarnings({"unchecked"})
    public T poll() {
        Node current = (Node) consumerHandle.getVolatile(this);
        Node nextNode = (Node) nextHandle.getVolatile(current);
        if(nextNode == null) {
            if(current != producerHandle.getVolatile(this)) {
                for( ; ; ) {
                    nextNode = (Node) nextHandle.getVolatile(current);
                    if(nextNode == null) {
                        Thread.onSpinWait();
                    }else {
                        break;
                    }
                }
            }else {
                return null;
            }
        }
        Object v = nextNode.current;
        nextNode.current = null;
        nextHandle.setRelease(current, current);
        consumerHandle.setRelease(this, nextNode);
        return (T) v;
    }
}
