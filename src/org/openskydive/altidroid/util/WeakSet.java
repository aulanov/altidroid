// Copyright 2012-2013 Andrey Ulanov
//
// This file is part of Altidroid.
//
// Altidroid is free software: you can redistribute it and/or modify
// it under the terms of the GNU General Public License as published by
// the Free Software Foundation, either version 3 of the License, or
// (at your option) any later version.
//
// Altidroid is distributed in the hope that it will be useful,
// but WITHOUT ANY WARRANTY; without even the implied warranty of
// MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
// GNU General Public License for more details.
//
// You should have received a copy of the GNU General Public License
// along with Altidroid.  If not, see <http://www.gnu.org/licenses/>.
package org.openskydive.altidroid.util;

import java.lang.ref.WeakReference;
import java.util.Collection;
import java.util.HashSet;
import java.util.Iterator;
import java.util.NoSuchElementException;
import java.util.Set;

public class WeakSet<E> implements Set<E> {
    Set<MyWeakReference<E>> mContent;

    private static class MyWeakReference<E2> extends WeakReference<E2> {
        public MyWeakReference(E2 object) {
            super(object);
        }

        @Override
        public boolean equals(Object o) {
            if (!(o instanceof MyWeakReference<?>)) {
                return false;
            }
            return ((MyWeakReference<?>) o).get() == get();
        }

        @Override
        public int hashCode() {
            return get() == null ? null : get().hashCode();
        }
    }

    private class WeakIterator<E2> implements Iterator<E2> {
        Iterator<MyWeakReference<E2>> mContentIterator;
        E2 next;

        public WeakIterator(Iterator<MyWeakReference<E2>> contentIterator) {
            mContentIterator = contentIterator;
        }

        public boolean hasNext() {
            if (next != null) {
                return true;
            }
            while (next == null && mContentIterator.hasNext()) {
                next = mContentIterator.next().get();
            }
            return next != null;
        }

        public E2 next() {
            while (next == null && mContentIterator.hasNext()) {
                next = mContentIterator.next().get();
            }
            if (next == null) {
                throw new NoSuchElementException();
            }
            E2 toReturn = next;
            next = null;
            return toReturn;
        }

        public void remove() {
            throw new UnsupportedOperationException();
        }
    }

    public WeakSet() {
        mContent = new HashSet<MyWeakReference<E>>();
    }

    public boolean add(E object) {
        return mContent.add(new MyWeakReference<E>(object));
    }

    public boolean addAll(Collection<? extends E> collection) {
        throw new UnsupportedOperationException();
    }

    public void clear() {
        mContent.clear();
    }

    public boolean contains(Object object) {
        Iterator<MyWeakReference<E>> i = mContent.iterator();
        while (i.hasNext()) {
            WeakReference<E> item = i.next();
            if (item.get() == object) {
                return true;
            }
        }
        return false;
    }

    public boolean isEmpty() {
        return mContent.isEmpty();
    }

    public Iterator<E> iterator() {
        return new WeakIterator<E>(mContent.iterator());
    }

    public int size() {
        return mContent.size();
    }

    public boolean containsAll(Collection<?> arg0) {
        throw new UnsupportedOperationException();
    }

    @SuppressWarnings("unchecked")
    public boolean remove(Object object) {
        return mContent.remove(new MyWeakReference<E>((E) object));
    }

    public boolean removeAll(Collection<?> arg0) {
        throw new UnsupportedOperationException();
    }

    public boolean retainAll(Collection<?> arg0) {
        throw new UnsupportedOperationException();
    }

    public Object[] toArray() {
        throw new UnsupportedOperationException();
    }

    public <T> T[] toArray(T[] array) {
        throw new UnsupportedOperationException();
    }
}
