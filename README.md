## 前言

Java8新增的Stream API是一个强大的特性，它可以简化集合中的常用操作，包括过滤、分组等。下面就来实现一个简易版的Stream。

从表面上看，流似乎和列表很接近，但实际上它们有着本质的区别。

* 列表是多个元素的容器，当列表被创建出来时，它里面的每个元素也已经被创建出来了。

* 流是一种计算结构，它封装了内部元素如何产生的计算过程，但是并没有包含实际的元素数据。换句话说，当一个流被创建出来时，它内部的元素并没有被创建，但是我们可以通过调用流的方法来按顺序生成每个元素。

所以，流具有惰性计算的特性，它可以表示普通列表无法表示的一些结构，如无限流。

## 流的定义

流的定义看起来很像链表，一个流由两部分组成：第一个元素（first）和剩余元素组成的流（remain）。定义如下：
```java
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
}
```

这种递归的定义非常有利于使用递归算法来操作流。下面可以看到，流的大多数相关操作都是用递归算法实现的。

假设我们已经有了一个流，那么如何获取流中的元素呢？首先调用`first`来获取第一个元素，然后调用`remain().first()`来获取第二个元素，依此类推：

```java
Stream<Integer> stream = ...
Integer first  = stream.first(); // 第一个元素
Integer second = stream.remain().first(); // 第二个元素
Integer third  = stream.remain().remain().first(); // 第三个元素

// 依此类推...
```

当然，我们不会用这种方法来访问流中的元素。具体如何访问，请继续往下看。

## 空流

空流是最简单的流，无法从空流中获取任何元素。空流也标志着一个流的结束。下面是空流的实现：

```java
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
```

## 有限流的生成

有限流可以通过多种方式生成，包括从数组生成、从迭代器生成、从集合生成。

### 从数组生成流

```java
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
```

### 从迭代器生成流

```java
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
```

### 从集合生成流

```java
/**
 * 从集合生成流
 * @param collection 集合
 * @param <T> 元素类型
 * @return 流
 */
static <T> Stream<T> fromCollection(Collection<T> collection) {
    return fromIterator(collection.iterator());
}
```

### 示例

```java
Stream<Integer> s1 = Stream.of(1, 2, 3); // 从数组生成
Stream<Integer> s2 = Stream.fromIterator(List.of(1, 2, 3).iterator()); // 从迭代器生成
Stream<Integer> s3 = Stream.fromCollection(Set.of(1, 2, 3)); // 从集合生成
```

## 无限流的生成

无限流意味着流中的元素个数没有限制，也就是永远都不会结束，所以`end`方法调用永远为`false`。有以下两种方法生成无限流。

### 从工厂方法生成流

```java
/**
 * 从工厂方法生成流
 * @param supplier 生成流中元素的工厂方法
 * @param <T> 元素类型
 * @return 流
 */
static <T> Stream<T> fromSupplier(Supplier<T> supplier) {
    return create(supplier, () -> fromSupplier(supplier));
}
```

### 从生成器生成流

```java
/**
 * 迭代生成流
 * @param initial 初始值
 * @param generator 生成器
 * @param <T> 元素类型
 * @return 流
 */
static <T> Stream<T> fromGenerator(T initial, UnaryOperator<T> generator) {
    return create(() -> initial, () -> generate(generator.apply(initial), generator));
}
```
### 示例

```java
Stream<Integer> s1 = Stream.fromSupplier(() -> 1); // 无限个1组成的流
Stream<Integer> s2 = Stream.fromGenerator(1, n -> n + 1); // 全体自然数组成的流
```

## 遍历流中的元素

知道了如何创建流，那么如何遍历或输出流中的元素呢？可以实现下面的`forEach`方法：

```java
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
```

然后就可以像下面这样输出流中的元素：

```java
Stream<Integer> s = Stream.of(1, 2, 3, 4, 5);
s.forEach(System.out::println); // 输出1 2 3 4 5
```

## 流的截断和偏移

上面的`forEach`方法只适用于有限流，如果在无限流上调用`forEach`方法，会导致死循环。所以，我们需要对无限流进行截取操作，这样就能做到遍历无限流的一部分。

```java
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
```

`limit`用于提取流的前n个元素，`skip`用于忽略流的前n个元素，有了这两个方法，我们就能随心所欲地截取任何流中的任意一段。

## 流的变换操作

熟悉Java8 Stream API的读者一定用过`map`和`filter`这两个常用的流操作，下面我们就来实现它们。

### map

`map`用于对流中的所有元素进行转换操作。

```java
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
```

### filter

`filter`用于过滤流中的元素。

```java
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
```

### 示例

```java
Stream<String> s = Stream.of(1, 2, 3, 4, 5, 6)
        .filter(n -> n % 2 == 0) // 2, 4, 6
        .map(n -> "hello " + n); // hello 2, hello 4, hello 6
```

## 流的聚合操作

有时候我们像将整个流聚合成某种数据结构，如列表、集合等，这就需要用到流的聚合操作。

### collect

对流进行自定义聚合操作。

```java
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
```

### toList

将流转换成列表。

```java
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
```

### toSet

将流转换成集合。

```java
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
```

### toMap

将流转换成`Map`。

```java
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
```

### count

对流中的元素进行计数。

```java
/**
 * 获取流中元素个数
 * @return 元素个数
 */
default int count() {
    return collect(0, (cnt, e) -> cnt + 1);
}
```

## 流的高级操作

下面是流的一些高级操作。

### concat

`concat`用于将两个流首尾连接在一起。

```java
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
 * 首尾连接两个流
 * @param s 要连接的流
 * @return 流
 */
default Stream<T> concat(Stream<T> s) {
    return concat(this, s);
}
```

示例：

```java
Stream<Integer> s1 = Stream.of(1, 2, 3, 4);
Stream<Integer> s2 = Stream.of(5, 6, 7);
Stream<Integer> s = s1.concat(s2); // 1, 2, 3, 4, 5, 6, 7
```

### interleave

`interleave`用于将两个流交错连接在一起。

```java
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
 * 交错连接两个流
 * @param s 要连接的流
 * @return 流
 */
default Stream<T> interleave(Stream<T> s) {
    return interleave(this, s);
}
```

示例：

```java
Stream<Integer> s1 = Stream.of(1, 3, 5, 7);
Stream<Integer> s2 = Stream.of(2, 4, 6);
Stream<Integer> s = s1.interleave(s2); // 1, 2, 3, 4, 5, 6, 7
```

### flatMap

`flatMap`用于将流中的每个元素都映射成一个流，然后将所有流连接起来。

```java
/**
 * 扁平化流
 * @param mapper 元素到流的映射器
 * @param <U> 扁平化后的元素类型
 * @return 流
 */
default <U> Stream<U> flatMap(Function<T, Stream<U>> mapper) {
    return collect(empty(), (s, e) -> s.concat(mapper.apply(e)));
}
```

示例：

```java
Stream<Integer> s = Stream.of(10, 20)
        .flatMap(n -> Stream.of(n + 1, n + 2, n + 3)); // 11, 12, 13, 21, 22, 23
```

## 后记

完整代码：[https://github.com/byx2000/simple-stream](https://github.com/byx2000/simple-stream)