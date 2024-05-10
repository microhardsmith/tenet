package cn.zorcc.common.structure;

import cn.zorcc.common.Constants;
import cn.zorcc.common.ExceptionType;
import cn.zorcc.common.exception.FrameworkException;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.List;

/**
 *   HashMap with int as key type, fixed size, better performance because of no auto-boxing
 *   TODO after value types got published, this class might be considered to be rewrite
 */
public sealed interface IntMap<T> permits IntMap.IntLinkedMap, IntMap.IntTreeMap {

    /**
     *   Create an IntMap using linked nodes, O(1) insertion, O(n) search
     */
    static <T> IntMap<T> newLinkedMap(int size) {
        if(Integer.bitCount(size) != 1) {
            throw new FrameworkException(ExceptionType.CONTEXT, "Size must be power of 2");
        }
        return new IntLinkedMap<>(size);
    }

    /**
     *   Create an IntMap using Red-Black tree, O(log(n)) insertion, O(log(n)) search
     */
    static <T> IntMap<T> newTreeMap(int size) {
        if(Integer.bitCount(size) != 1) {
            throw new FrameworkException(ExceptionType.CONTEXT, "Size must be power of 2");
        }
        return new IntTreeMap<>(size);
    }

    /**
     *   Get an element from current IntMap, return null if it doesn't exist
     */
    T get(int identifier);

    /**
     *   Put a new element in the IntMap, the element must not exist, or data would be corrupted
     */
    void put(int identifier, T value);

    /**
     *   Replace an element, the element must already exist in the IntMap, or an exception would be thrown
     */
    void replace(int identifier, T oldValue, T newValue);

    /**
     *   Remove an element from current IntMap, return if removed successfully
     *   Note this function compares the identity using == , so the object must be the exact one using get()
     */
    boolean remove(int identifier, T value);

    /**
     *   Return the current element count
     */
    int count();

    /**
     *   Transforming current IntMap into a new List
     */
    List<T> asList();

    default boolean isEmpty() {
        return count() == 0;
    }

    final class IntLinkedMap<T> implements IntMap<T> {
        private final Node<T>[] nodes;
        private final int mask;
        private int size;

        @SuppressWarnings("unchecked")
        IntLinkedMap(int size) {
            this.nodes = new Node[size];
            this.mask = size - 1;
            this.size = 0;
        }

        private static final class Node<T> {
            int identifier;
            T value;
            Node<T> next;
        }

        @Override
        public T get(int identifier) {
            int index = identifier & mask;
            Node<T> cur = nodes[index];
            while (cur != null) {
                if(cur.identifier == identifier) {
                    return cur.value;
                }else {
                    cur = cur.next;
                }
            }
            return null;
        }

        @Override
        public void put(int identifier, T value) {
            Node<T> n = new Node<>();
            n.identifier = identifier;
            n.value = value;
            int slot = identifier & mask;
            n.next = nodes[slot];
            nodes[slot] = n;
            size++;
        }

        @Override
        public void replace(int identifier, T oldValue, T newValue) {
            int index = identifier & mask;
            Node<T> cur = nodes[index];
            while (cur != null) {
                if(cur.identifier == identifier) {
                    if(cur.value == oldValue) {
                        cur.value = newValue;
                        return ;
                    } else {
                        throw new FrameworkException(ExceptionType.CONTEXT, Constants.UNREACHED);
                    }
                }else {
                    cur = cur.next;
                }
            }
            throw new FrameworkException(ExceptionType.CONTEXT, Constants.UNREACHED);
        }

        @Override
        public boolean remove(int identifier, T value) {
            final int slot = identifier & mask;
            Node<T> prev = null, cur = nodes[slot];
            while (cur != null) {
                if(cur.identifier == identifier) {
                    if(cur.value == value) {
                        if(prev == null) {
                            nodes[slot] = cur.next;
                        }else {
                            prev.next = cur.next;
                        }
                        size--;
                        return true;
                    }else {
                        return false;
                    }
                }else {
                    prev = cur;
                    cur = cur.next;
                }
            }
            return false;
        }

        @Override
        public int count() {
            return size;
        }

        @Override
        public List<T> asList() {
            List<T> result = new ArrayList<>();
            for (Node<T> n : nodes) {
                Node<T> ptr = n;
                while (ptr != null) {
                    result.add(ptr.value);
                    ptr = ptr.next;
                }
            }
            return result;
        }
    }

    final class IntTreeMap<T> implements IntMap<T> {
        private final Entry<T>[] entries;
        private final int mask;
        private int size;

        @SuppressWarnings("unchecked")
        IntTreeMap(int size) {
            this.entries = new Entry[size];
            this.mask = size - 1;
            this.size = 0;
        }

        private static final class Entry<T> {
            int identifier;
            T value;
            Entry<T> left;
            Entry<T> right;
            Entry<T> parent;
            boolean color = true; // BLACK for true, RED for false
        }

        @Override
        public T get(int identifier) {
            Entry<T> current = entries[identifier & mask];
            while (current != null) {
                int i = current.identifier;
                if(i == identifier) {
                    return current.value;
                }else if(i < identifier) {
                    current = current.right;
                }else {
                    current = current.left;
                }
            }
            return null;
        }

        @Override
        public void put(int identifier, T value) {
            int slot = identifier & mask;
            if(entries[slot] == null) {
                Entry<T> entry = new Entry<>();
                entry.identifier = identifier;
                entry.value = value;
                entries[slot] = entry;
                size++;
                return ;
            }
            Entry<T> current = entries[slot];
            Entry<T> parent;
            do {
                parent = current;
                if(identifier < current.identifier) {
                    current = current.left;
                }else if(identifier > current.identifier) {
                    current = current.right;
                }else {
                    throw new FrameworkException(ExceptionType.CONTEXT, Constants.UNREACHED);
                }
            } while (current != null);
            addEntry(slot, identifier, value, parent, identifier < parent.identifier);
        }

        private void addEntry(int slot, int identifier, T value, Entry<T> parent, boolean addToLeft) {
            Entry<T> entry = new Entry<>();
            entry.identifier = identifier;
            entry.value = value;
            entry.parent = parent;
            if (addToLeft) {
                parent.left = entry;
            }else {
                parent.right = entry;
            }
            fixAfterInsertion(slot, entry);
            size++;
        }

        private void fixAfterInsertion(int slot, Entry<T> entry) {
            entry.color = false;
            while (entry != null && entry != entries[slot] && !entry.parent.color) {
                if (parentOf(entry) == leftOf(parentOf(parentOf(entry)))) {
                    Entry<T> y = rightOf(parentOf(parentOf(entry)));
                    if(colorOf(y)) {
                        if (entry == rightOf(parentOf(entry))) {
                            entry = parentOf(entry);
                            rotateLeft(slot, entry);
                        }
                        setColor(parentOf(entry), true);
                        setColor(parentOf(parentOf(entry)), false);
                        rotateRight(slot, parentOf(parentOf(entry)));
                    } else {
                        setColor(parentOf(entry), true);
                        setColor(y, true);
                        setColor(parentOf(parentOf(entry)), false);
                        entry = parentOf(parentOf(entry));
                    }
                } else {
                    Entry<T> y = leftOf(parentOf(parentOf(entry)));
                    if(colorOf(y)) {
                        if (entry == leftOf(parentOf(entry))) {
                            entry = parentOf(entry);
                            rotateRight(slot, entry);
                        }
                        setColor(parentOf(entry), true);
                        setColor(parentOf(parentOf(entry)), false);
                        rotateLeft(slot, parentOf(parentOf(entry)));
                    } else {
                        setColor(parentOf(entry), true);
                        setColor(y, true);
                        setColor(parentOf(parentOf(entry)), false);
                        entry = parentOf(parentOf(entry));
                    }
                }
            }
            entries[slot].color = true;
        }

        private static <T> boolean colorOf(Entry<T> p) {
            return (p == null || p.color);
        }

        private static <T> Entry<T> parentOf(Entry<T> entry) {
            return (entry == null ? null: entry.parent);
        }

        private static <T> void setColor(Entry<T> entry, boolean c) {
            if (entry != null) {
                entry.color = c;
            }
        }

        private static <T> Entry<T> leftOf(Entry<T> entry) {
            return (entry == null) ? null: entry.left;
        }

        private static <T> Entry<T> rightOf(Entry<T> entry) {
            return (entry == null) ? null: entry.right;
        }

        private void rotateLeft(int slot, Entry<T> entry) {
            if (entry != null) {
                Entry<T> r = entry.right;
                entry.right = r.left;
                if (r.left != null) {
                    r.left.parent = entry;
                }
                r.parent = entry.parent;
                if (entry.parent == null) {
                    entries[slot] = r;
                } else if (entry.parent.left == entry) {
                    entry.parent.left = r;
                } else {
                    entry.parent.right = r;
                }
                r.left = entry;
                entry.parent = r;
            }
        }

        private void rotateRight(int slot, Entry<T> entry) {
            if (entry != null) {
                Entry<T> l = entry.left;
                entry.left = l.right;
                if (l.right != null) {
                    l.right.parent = entry;
                }
                l.parent = entry.parent;
                if (entry.parent == null) {
                    entries[slot] = l;
                } else if (entry.parent.right == entry) {
                    entry.parent.right = l;
                } else {
                    entry.parent.left = l;
                }
                l.right = entry;
                entry.parent = l;
            }
        }

        @Override
        public void replace(int identifier, T oldValue, T newValue) {
            Entry<T> current = entries[identifier & mask];
            while (current != null) {
                int i = current.identifier;
                if(i == identifier) {
                    if(current.value == oldValue) {
                        current.value = newValue;
                        return ;
                    } else {
                        throw new FrameworkException(ExceptionType.CONTEXT, Constants.UNREACHED);
                    }
                }else if(i < identifier) {
                    current = current.right;
                }else {
                    current = current.left;
                }
            }
            throw new FrameworkException(ExceptionType.CONTEXT, Constants.UNREACHED);
        }

        @Override
        public boolean remove(int identifier, T value) {
            final int slot = identifier & mask;
            Entry<T> current = entries[slot];
            while (current != null) {
                int i = current.identifier;
                if(i == identifier) {
                    if(current.value == value) {
                        deleteEntry(slot, current);
                        return true;
                    }else {
                        return false;
                    }
                }else if(i < identifier) {
                    current = current.right;
                }else {
                    current = current.left;
                }
            }
            return false;
        }

        private void deleteEntry(int slot, Entry<T> p) {
            size--;
            if (p.left != null && p.right != null) {
                Entry<T> s = successor(p);
                p.identifier = s.identifier;
                p.value = s.value;
                p = s;
            }
            Entry<T> replacement = (p.left != null ? p.left : p.right);
            if(replacement != null) {
                replacement.parent = p.parent;
                if (p.parent == null) {
                    entries[slot] = replacement;
                }else if (p == p.parent.left) {
                    p.parent.left  = replacement;
                }else {
                    p.parent.right = replacement;
                }
                p.left = p.right = p.parent = null;
                if (p.color) {
                    fixAfterDeletion(slot, replacement);
                }
            }else if (p.parent == null) {
                entries[slot] = null;
            }else {
                if (p.color) {
                    fixAfterDeletion(slot, p);
                }
                if (p.parent != null) {
                    if (p == p.parent.left) {
                        p.parent.left = null;
                    }else if (p == p.parent.right) {
                        p.parent.right = null;
                    }
                    p.parent = null;
                }
            }
        }

        private void fixAfterDeletion(int slot, Entry<T> x) {
            while (x != entries[slot] && colorOf(x)) {
                if (x == leftOf(parentOf(x))) {
                    Entry<T> sib = rightOf(parentOf(x));
                    if (!colorOf(sib)) {
                        setColor(sib, true);
                        setColor(parentOf(x), false);
                        rotateLeft(slot, parentOf(x));
                        sib = rightOf(parentOf(x));
                    }
                    if (colorOf(leftOf(sib)) && colorOf(rightOf(sib))) {
                        setColor(sib, false);
                        x = parentOf(x);
                    } else {
                        if (colorOf(rightOf(sib))) {
                            setColor(leftOf(sib), true);
                            setColor(sib, false);
                            rotateRight(slot, sib);
                            sib = rightOf(parentOf(x));
                        }
                        setColor(sib, colorOf(parentOf(x)));
                        setColor(parentOf(x), true);
                        setColor(rightOf(sib), true);
                        rotateLeft(slot, parentOf(x));
                        x = entries[slot];
                    }
                } else {
                    Entry<T> sib = leftOf(parentOf(x));
                    if (!colorOf(sib)) {
                        setColor(sib, true);
                        setColor(parentOf(x), false);
                        rotateRight(slot, parentOf(x));
                        sib = leftOf(parentOf(x));
                    }
                    if (colorOf(rightOf(sib)) && colorOf(leftOf(sib))) {
                        setColor(sib, false);
                        x = parentOf(x);
                    } else {
                        if (colorOf(leftOf(sib))) {
                            setColor(rightOf(sib), true);
                            setColor(sib, false);
                            rotateLeft(slot, sib);
                            sib = leftOf(parentOf(x));
                        }
                        setColor(sib, colorOf(parentOf(x)));
                        setColor(parentOf(x), true);
                        setColor(leftOf(sib), true);
                        rotateRight(slot, parentOf(x));
                        x = entries[slot];
                    }
                }
            }
            setColor(x, true);
        }

        private static <T> Entry<T> successor(Entry<T> t) {
            if (t == null) {
                return null;
            }else if (t.right != null) {
                Entry<T> p = t.right;
                while (p.left != null) {
                    p = p.left;
                }
                return p;
            }else {
                Entry<T> p = t.parent;
                Entry<T> ch = t;
                while (p != null && ch == p.right) {
                    ch = p;
                    p = p.parent;
                }
                return p;
            }
        }

        @Override
        public int count() {
            return size;
        }

        @Override
        public List<T> asList() {
            int count = count();
            Deque<Entry<T>> deque = new ArrayDeque<>(count);
            for (Entry<T> n : entries) {
                if(n != null) {
                    deque.addLast(n);
                }
            }
            List<T> result = new ArrayList<>(count);
            while (!deque.isEmpty()) {
                Entry<T> entry = deque.pollFirst();
                Entry<T> left = entry.left;
                if(left != null) {
                    deque.addLast(left);
                }
                Entry<T> right = entry.right;
                if(right != null) {
                    deque.addLast(right);
                }
                result.add(entry.value);
            }
            return result;
        }
    }
}
