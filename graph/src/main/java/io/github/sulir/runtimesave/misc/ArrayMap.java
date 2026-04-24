package io.github.sulir.runtimesave.misc;

import org.jetbrains.annotations.NotNull;

import java.util.*;
import java.util.function.BiConsumer;

public class ArrayMap<V> extends AbstractMap<Integer, V> implements SortedMap<Integer, V> {
    private Object[] values = new Object[8];
    private int first = Integer.MAX_VALUE;
    private int last = Integer.MIN_VALUE;
    private int size;

    @Override
    public V get(Object key) {
        int index = (int) key;
        return index < values.length ? elementAt(index) : null;
    }

    @Override
    public V put(Integer key, V value) {
        int index = key;
        if (index >= values.length)
            values = Arrays.copyOf(values, Math.max(index + 1, values.length * 2));
        if (index < first)
            first = index;
        if (index > last)
            last = index;

        V old = elementAt(index);
        if (old == null)
            size++;
        values[index] = value;
        return old;
    }

    @Override
    public void forEach(BiConsumer<? super Integer, ? super V> action) {
        for (int i = first; i <= last; i++) {
            V value = elementAt(i);
            if (value != null)
                action.accept(i, value);
        }
    }

    @Override
    public Integer firstKey() {
        if (first == Integer.MAX_VALUE)
            throw new NoSuchElementException();
        return first;
    }

    @Override
    public Integer lastKey() {
        if (last == Integer.MIN_VALUE)
            throw new NoSuchElementException();
        return last;
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
                    private int i = first;

                    @Override
                    public boolean hasNext() {
                        while (i <= last && values[i] == null)
                            i++;
                        return i <= last;
                    }

                    @Override
                    public Entry<Integer, V> next() {
                        if (!hasNext())
                            throw new NoSuchElementException();
                        return new SimpleEntry<>(i, elementAt(i++));
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
