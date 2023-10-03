package cn.zorcc.common.network;

import cn.zorcc.common.Constants;
import cn.zorcc.common.enums.ExceptionType;
import cn.zorcc.common.exception.FrameworkException;

import java.util.ArrayList;
import java.util.List;

public final class ActorMap {
    private final Node[] nodes;
    private final int mask;

    public ActorMap(int size) {
        this.nodes = new Node[size];
        this.mask = size - Constants.ONE;
        if((size & mask) != Constants.ZERO) {
            throw new FrameworkException(ExceptionType.NETWORK, "Actor mapSize must be power of 2");
        }
        for(int index = Constants.ZERO; index < nodes.length; index++) {
            Node header = new Node();
            header.val = Constants.ZERO;
            nodes[index] = header;
        }
    }

    public Actor get(int val) {
        int slot = val & mask;
        Node header = nodes[slot];
        Node current = header.next;
        while (current != null) {
            if(current.val == val) {
                return current.actor;
            }else {
                current = current.next;
            }
        }
        return null;
    }

    public void put(int val, Actor actor) {
        Node n = new Node();
        n.val = val;
        n.actor = actor;
        int slot = val & mask;
        Node header = nodes[slot];
        Node current = header;
        while (current.next != null) {
            current = current.next;
        }
        current.next = n;
        n.prev = current;
        header.val = header.val + Constants.ONE;
    }

    public void replace(int val, Actor actor) {
        int slot = val & mask;
        Node header = nodes[slot];
        Node current = header.next;
        while (current != null) {
            if(current.val == val) {
                current.actor = actor;
                return ;
            }else {
                current = current.next;
            }
        }
        throw new FrameworkException(ExceptionType.NETWORK, Constants.UNREACHED);
    }

    public boolean remove(int val, Actor actor) {
        int slot = val & mask;
        Node header = nodes[slot];
        Node current = header.next;
        while (current.val != val) {
            current = current.next;
            if(current == null) {
                return false;
            }
        }
        if(current.actor != actor) {
            return false;
        }
        Node prev = current.prev;
        Node next = current.next;
        prev.next = next;
        if(next != null) {
            next.prev = prev;
        }
        current.prev = null;
        current.next = null;
        header.val = header.val - Constants.ONE;
        return true;
    }

    public List<Actor> toList() {
        List<Actor> result = new ArrayList<>();
        for (Node header : nodes) {
            int len = header.val;
            Node current = header.next;
            for(int i = Constants.ZERO; i < len; i++) {
                result.add(current.actor);
                current = current.next;
            }
        }
        return result;
    }

    private static class Node {
        private int val;
        private Actor actor;
        private Node prev;
        private Node next;
    }
}
