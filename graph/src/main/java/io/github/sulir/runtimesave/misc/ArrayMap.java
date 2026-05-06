package io.github.sulir.runtimesave.misc;

import org.jetbrains.annotations.NotNull;

import java.util.*;
import java.util.function.BiConsumer;

public class ArrayMap<V> extends AbstractMap<Integer, V> implements SortedMap<Integer, V> {
    private final Object[] values;
    private int size;
    private int last = -1;

    public ArrayMap(int length) {
        values = new Object[length];
    }

    @Override
    public V get(Object key) {
        return elementAt((int) key);
    }

    @Override
    public V put(Integer key, V value) {
        V old = elementAt(key);
        values[key] = value;
        if (old == null)
            size++;
        if (value == null)
            size--;
        if (key > last)
            last = key;
        return old;
    }

    @Override
    public void forEach(BiConsumer<? super Integer, ? super V> action) {
        for (int i = 0; i <= last; i++) {
            V value = elementAt(i);
            if (value != null)
                action.accept(i, value);
        }
    }

    @Override
    public Integer firstKey() {
        for (int i = 0; i <= last; i++)
            if (values[i] != null)
                return i;
        throw new NoSuchElementException();
    }

    @Override
    public Integer lastKey() {
        for (int i = last; i >= 0; i--)
            if (values[i] != null)
                return i;
        throw new NoSuchElementException();
    }

    @Override
    public int size() {
        return size;
    }

    @Override
    public @NotNull Set<Entry<Integer, V>> entrySet() {
        return new AbstractSet<>() {
            @Override
            public @NotNull Iterator<Entry<Integer, V>> iterator() {
                return new Iterator<>() {
                    private int next = -1;

                    { findNext(); }

                    @Override
                    public boolean hasNext() {
                        return next <= last;
                    }

                    @Override
                    public Entry<Integer, V> next() {
                        if (!hasNext())
                            throw new NoSuchElementException();
                        int current = next;
                        findNext();
                        return new SimpleImmutableEntry<>(current, elementAt(current));
                    }

                    private void findNext() {
                        do {
                            next++;
                        } while (next <= last && values[next] == null);
                    }
                };
            }

            @Override
            public int size() {
                return size;
            }
        };
    }

    @Override
    public Comparator<? super Integer> comparator() {
        return null;
    }

    @Override
    public @NotNull SortedMap<Integer, V> subMap(Integer fromKey, Integer toKey) {
        throw new UnsupportedOperationException();
    }

    @Override
    public @NotNull SortedMap<Integer, V> headMap(Integer toKey) {
        throw new UnsupportedOperationException();
    }

    @Override
    public @NotNull SortedMap<Integer, V> tailMap(Integer fromKey) {
        throw new UnsupportedOperationException();
    }

    @SuppressWarnings("unchecked")
    private V elementAt(int index) {
        return (V) values[index];
    }
}
