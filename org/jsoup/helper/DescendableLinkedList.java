/*
 * Decompiled with CFR 0_114.
 */
package org.jsoup.helper;

import java.util.Iterator;
import java.util.LinkedList;
import java.util.ListIterator;

/*
 * This class specifies class file version 49.0 but uses Java 6 signatures.  Assumed Java 6.
 */
public class DescendableLinkedList<E>
extends LinkedList<E> {
    @Override
    public void push(E e) {
        this.addFirst(e);
    }

    @Override
    public E peekLast() {
        return this.size() == 0 ? null : (E)this.getLast();
    }

    @Override
    public E pollLast() {
        return this.size() == 0 ? null : (E)this.removeLast();
    }

    @Override
    public Iterator<E> descendingIterator() {
        return new DescendingIterator(this.size());
    }

    /*
     * This class specifies class file version 49.0 but uses Java 6 signatures.  Assumed Java 6.
     */
    private class DescendingIterator<E>
    implements Iterator<E> {
        private final ListIterator<E> iter;

        private DescendingIterator(int index) {
            this.iter = DescendableLinkedList.this.listIterator(index);
        }

        @Override
        public boolean hasNext() {
            return this.iter.hasPrevious();
        }

        @Override
        public E next() {
            return this.iter.previous();
        }

        @Override
        public void remove() {
            this.iter.remove();
        }
    }

}

