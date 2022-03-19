package byx.project.stream;

import java.util.*;
import java.util.function.*;

public interface Stream<T> {
    /**
     * 流中第一个元素
     */
    T first();

    /**
     * 剩余元素组成的流
     */
    Stream<T> remain();

    /**
     * 创建流
     * @param firstSupplier 第一个元素的工厂
     * @param remainSupplier 剩余元素组成的流的工厂
     * @param <T> 元素类型
     * @return 流
     */
    static <T> Stream<T> create(Supplier<T> firstSupplier, Supplier<Stream<T>> remainSupplier) {
        return new Stream<>() {
            @Override
            public T first() {
                return firstSupplier.get();
            }

            @Override
            public Stream<T> remain() {
                return remainSupplier.get();
            }
        };
    }

    Stream<?> EMPTY = create(
            () -> {throw new IllegalStateException("当前流已结束");},
            () -> {throw new IllegalStateException("当前流已结束");}
    );

    /**
     * 获取空流
     */
    @SuppressWarnings("unchecked")
    static <T> Stream<T> empty() {
        return (Stream<T>) EMPTY;
    }

    /**
     * 判断当前流是否结束
     */
    default boolean end() {
        return this == EMPTY;
    }

    /**
     * 从数组生成流
     * @param arr 数组
     * @param <T> 元素类型
     * @return 流
     */
    @SafeVarargs
    static <T> Stream<T> of(T... arr) {
        return fromArray(0, arr);
    }

    /**
     * 从数组和起始索引生成流
     * @param startIndex 起始索引
     * @param arr 数组
     * @param <T> 元素类型
     * @return 流
     */
    static <T> Stream<T> fromArray(int startIndex, T[] arr) {
        return startIndex == arr.length
                ? empty()
                : create(() -> arr[startIndex], () -> fromArray(startIndex + 1, arr));
    }

    /**
     * 从迭代器生成流
     * @param iterator 迭代器
     * @param <T> 元素类型
     * @return 流
     */
    static <T> Stream<T> fromIterator(Iterator<T> iterator) {
        return iterator.hasNext()
                ? create(iterator::next, () -> fromIterator(iterator))
                : empty();
    }

    /**
     * 从集合生成流
     * @param collection 集合
     * @param <T> 元素类型
     * @return 流
     */
    static <T> Stream<T> fromCollection(Collection<T> collection) {
        return fromIterator(collection.iterator());
    }

    /**
     * 从工厂方法生成流
     * @param supplier 生成流中元素的工厂方法
     * @param <T> 元素类型
     * @return 流
     */
    static <T> Stream<T> fromSupplier(Supplier<T> supplier) {
        return create(supplier, () -> fromSupplier(supplier));
    }

    /**
     * 迭代生成流
     * @param initial 初始值
     * @param generator 生成器
     * @param <T> 元素类型
     * @return 流
     */
    static <T> Stream<T> fromGenerator(T initial, UnaryOperator<T> generator) {
        return create(() -> initial, () -> fromGenerator(generator.apply(initial), generator));
    }

    /**
     * 首尾连接两个流
     * s1[0] -> s1[1] -> s2[2] ->  ... -> s2[0] -> s2[1] -> s2[2] -> ...
     * @param s1 s1
     * @param s2 s2
     * @param <T> 元素类型
     * @return 流
     */
    static <T> Stream<T> concat(Stream<T> s1, Stream<T> s2) {
        return s1.end()
                ? s2
                : create(s1::first, () -> concat(s1.remain(), s2));
    }

    /**
     * 交错连接两个流
     * s1[0] -> s2[0] -> s1[1] -> s2[1] -> ...
     * @param s1 s1
     * @param s2 s2
     * @param <T> 元素类型
     * @return 流
     */
    static <T> Stream<T> interleave(Stream<T> s1, Stream<T> s2) {
        return s1.end()
                ? s2
                : create(s1::first, () -> interleave(s2, s1.remain()));
    }

    /**
     * 流的聚合操作
     * @param initial 初始值
     * @param accumulator 聚合操作
     * @param <U> 聚合后的类型
     * @return 流
     */
    default <U> U collect(U initial, BiFunction<U, T, U> accumulator) {
        U result = initial;
        Stream<T> s = this;
        while (!s.end()) {
            result = accumulator.apply(result, s.first());
            s = s.remain();
        }
        return result;
    }

    /**
     * 将流转换成列表
     * @return 列表
     */
    default List<T> toList() {
        return collect(new ArrayList<>(), (list, e) -> {
            list.add(e);
            return list;
        });
    }

    /**
     * 将流转换成集合
     * @return 集合
     */
    default Set<T> toSet() {
        return collect(new HashSet<>(), (set, e) -> {
            set.add(e);
            return set;
        });
    }

    /**
     * 将流转换成map
     * @param keyGenerator key生成器
     * @param valueGenerator value生成器
     * @param <K> key的类型
     * @param <V> value的类型
     * @return map
     */
    default <K, V> Map<K, V> toMap(Function<T, K> keyGenerator, Function<T, V> valueGenerator) {
        return collect(new HashMap<>(), (map, e) -> {
            map.put(keyGenerator.apply(e), valueGenerator.apply(e));
            return map;
        });
    }

    /**
     * 获取流中元素个数
     * @return 元素个数
     */
    default int count() {
        return collect(0, (cnt, e) -> cnt + 1);
    }

    /**
     * 遍历流中所有元素
     * @param consumer 遍历操作
     */
    default void forEach(Consumer<T> consumer) {
        Stream<T> s = this;
        while (!s.end()) {
            consumer.accept(s.first());
            s = s.remain();
        }
    }

    /**
     * 截取流中前n个元素
     * @param n 要截取的元素个数
     * @return 流
     */
    default Stream<T> limit(int n) {
        return n <= 0 || end()
                ? empty()
                : create(this::first, () -> remain().limit(n - 1));
    }

    /**
     * 跳过流中的元素
     * @param n 跳过的个数
     * @return 流
     */
    default Stream<T> skip(int n) {
        return end() || n <= 0
                ? this
                : remain().skip(n - 1);
    }

    /**
     * 映射流中的元素
     * @param mapper 映射器
     * @param <U> 映射后的元素类型
     * @return 流
     */
    default <U> Stream<U> map(Function<T, U> mapper) {
        return end()
                ? empty()
                : create(() -> mapper.apply(first()), () -> remain().map(mapper));
    }

    /**
     * 过滤流中的元素
     * @param predicate 断言
     * @return 流
     */
    default Stream<T> filter(Predicate<T> predicate) {
        if (end()) {
            return empty();
        }
        T e = first();
        if (predicate.test(e)) {
            return Stream.create(() -> e, () -> remain().filter(predicate));
        } else {
            return remain().filter(predicate);
        }
    }

    /**
     * 首尾连接两个流
     * @param s 要连接的流
     * @return 流
     */
    default Stream<T> concat(Stream<T> s) {
        return concat(this, s);
    }

    /**
     * 交错连接两个流
     * @param s 要连接的流
     * @return 流
     */
    default Stream<T> interleave(Stream<T> s) {
        return interleave(this, s);
    }

    /**
     * 扁平化流
     * @param mapper 元素到流的映射器
     * @param <U> 扁平化后的元素类型
     * @return 流
     */
    default <U> Stream<U> flatMap(Function<T, Stream<U>> mapper) {
        return collect(empty(), (s, e) -> s.concat(mapper.apply(e)));
    }
}
