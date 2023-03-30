package gearth.extensions.parsers.stuffdata;

import java.util.*;
import java.util.function.Consumer;
import java.util.function.Predicate;
import java.util.function.UnaryOperator;
import java.util.stream.Stream;

/**
 * Wrapper class to make IntArrayStuffData and StringArrayStuffData behave like a List
 * @param <T> Value class
 */
public class ArrayStuffData<T> extends StuffDataBase implements List<T> {
    protected List<T> values = new ArrayList<>();

    public ArrayStuffData() {
        super();
    }

    public ArrayStuffData(int uniqueSerialNumber, int uniqueSerialSize) {
        super(uniqueSerialNumber, uniqueSerialSize);
    }

    @Override
    public int size() {
        return values.size();
    }

    @Override
    public boolean isEmpty() {
        return values.isEmpty();
    }

    @Override
    public boolean contains(Object o) {
        return values.contains(o);
    }

    @Override
    public Iterator<T> iterator() {
        return values.iterator();
    }

    @Override
    public void forEach(Consumer<? super T> action) {
        values.forEach(action);
    }

    @Override
    public Object[] toArray() {
        return values.toArray();
    }

    @Override
    public <T1> T1[] toArray(T1[] a) {
        return values.toArray(a);
    }

    @Override
    public boolean add(T t) {
        return values.add(t);
    }

    @Override
    public boolean remove(Object o) {
        return values.remove(o);
    }

    @Override
    public boolean containsAll(Collection<?> c) {
        return values.containsAll(c);
    }

    @Override
    public boolean addAll(Collection<? extends T> c) {
        return values.addAll(c);
    }

    @Override
    public boolean addAll(int index, Collection<? extends T> c) {
        return values.addAll(index, c);
    }

    @Override
    public boolean removeAll(Collection<?> c) {
        return values.removeAll(c);
    }

    @Override
    public boolean removeIf(Predicate<? super T> filter) {
        return values.removeIf(filter);
    }

    @Override
    public boolean retainAll(Collection<?> c) {
        return values.retainAll(c);
    }

    @Override
    public void replaceAll(UnaryOperator<T> operator) {
        values.replaceAll(operator);
    }

    @Override
    public void sort(Comparator<? super T> c) {
        values.sort(c);
    }

    @Override
    public void clear() {
        values.clear();
    }

    @Override
    public T get(int index) {
        return values.get(index);
    }

    @Override
    public T set(int index, T element) {
        return values.set(index, element);
    }

    @Override
    public void add(int index, T element) {
        values.add(index, element);
    }

    @Override
    public T remove(int index) {
        return values.remove(index);
    }

    @Override
    public int indexOf(Object o) {
        return values.indexOf(o);
    }

    @Override
    public int lastIndexOf(Object o) {
        return values.lastIndexOf(o);
    }

    @Override
    public ListIterator<T> listIterator() {
        return values.listIterator();
    }

    @Override
    public ListIterator<T> listIterator(int index) {
        return values.listIterator(index);
    }

    @Override
    public List<T> subList(int fromIndex, int toIndex) {
        return values.subList(fromIndex, toIndex);
    }

    @Override
    public Spliterator<T> spliterator() {
        return values.spliterator();
    }

    @Override
    public Stream<T> stream() {
        return values.stream();
    }

    @Override
    public Stream<T> parallelStream() {
        return values.parallelStream();
    }
}
