package byx.project.stream;

import org.junit.jupiter.api.Test;

import java.util.*;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.*;

public class StreamTest {
    @Test
    public void testEmptyStream() {
        Stream<?> s1 = Stream.empty();
        assertTrue(s1.end());
        assertTrue(s1.toList().isEmpty());
        Stream<Integer> s2 = Stream.empty();
        assertTrue(s2.end());
        assertTrue(s2.toList().isEmpty());
        Stream<String> s3 = Stream.empty();
        assertTrue(s3.end());
        assertTrue(s3.toList().isEmpty());
    }

    @Test
    public void testGenerateStreamFromArray() {
        Stream<Integer> s1 = Stream.of(1, 2, 3, 4, 5);
        assertEquals(List.of(1, 2, 3, 4, 5), s1.toList());
        Stream<String> s2 = Stream.of("abc", "def", "ghi");
        assertEquals(List.of("abc", "def", "ghi"), s2.toList());
        Stream<?> s3 = Stream.of();
        assertTrue(s3.end());
        assertTrue(s3.toList().isEmpty());
    }

    @Test
    public void testGenerateStreamFromSupplier() {
        Stream<Integer> s = Stream.fromSupplier(() -> 1);
        assertEquals(List.of(1, 1, 1), s.limit(3).toList());
    }

    @Test
    public void testGenerateStreamFromIterator() {
        Stream<String> s1 = Stream.fromIterator(List.of("aaa", "bbb", "ccc").iterator());
        assertEquals(List.of("aaa", "bbb", "ccc"), s1.toList());
        Stream<?> s2 = Stream.fromIterator(Collections.emptyIterator());
        assertTrue(s2.end());
        assertTrue(s2.toList().isEmpty());
    }

    @Test
    public void testGenerateStreamFromCollection() {
        Stream<Integer> s1 = Stream.fromCollection(List.of(2, 3, 5, 7, 11));
        assertEquals(List.of(2, 3, 5, 7, 11), s1.toList());
        Stream<String> s2 = Stream.fromCollection(Set.of("aa", "bb", "cc"));
        assertEquals(Set.of("aa", "bb", "cc"), new HashSet<>(s2.toList()));
    }

    @Test
    public void testGenerateStream() {
        Stream<Integer> s = Stream.fromGenerator(1, n -> n + 1);
        assertEquals(List.of(1, 2, 3), s.limit(3).toList());
    }

    @Test
    public void testLimit() {
        assertEquals(List.of(1, 2, 3), Stream.of(1, 2, 3, 4, 5).limit(3).toList());
        assertEquals(List.of(1, 2, 3, 4, 5), Stream.of(1, 2, 3, 4, 5).limit(100).toList());
        assertTrue(Stream.of(1, 2, 3, 4, 5).limit(0).toList().isEmpty());
        assertTrue(Stream.of(1, 2, 3, 4, 5).limit(-1).toList().isEmpty());
    }

    @Test
    public void testSkip() {
        Stream<Integer> s1 = Stream.of(1, 2, 3, 4);
        assertEquals(List.of(1, 2, 3, 4), s1.skip(-1).toList());
        assertEquals(List.of(1, 2, 3, 4), s1.skip(0).toList());
        assertEquals(List.of(3, 4), s1.skip(2).toList());
        assertTrue(s1.skip(4).end());
        assertTrue(s1.skip(4).toList().isEmpty());
        assertTrue(s1.skip(100).end());
        assertTrue(s1.skip(100).toList().isEmpty());
        assertTrue(Stream.empty().skip(2).end());
        assertTrue(Stream.empty().skip(2).toList().isEmpty());
        Stream<Integer> s2 = Stream.fromGenerator(1, n -> n + 1);
        assertEquals(List.of(3, 4, 5, 6, 7), s2.skip(2).limit(5).toList());
    }

    @Test
    public void testForEach() {
        Stream<Integer> s = Stream.of(1, 2, 3, 4, 5);
        AtomicInteger sum = new AtomicInteger(0);
        s.forEach(sum::addAndGet);
        assertEquals(15, sum.get());
    }

    @Test
    public void testToSet() {
        Stream<Integer> s = Stream.of(1, 2, 3, 2, 4, 1, 3);
        assertEquals(Set.of(1, 2, 3, 4), s.toSet());
    }

    @Test
    public void testToMap() {
        Map<Integer, String> map = Stream.of(1, 2, 3)
                .toMap(n -> n, String::valueOf);
        assertEquals(Map.of(
                1, "1",
                2, "2",
                3, "3"
        ), map);
    }

    @Test
    public void testCount() {
        assertEquals(5, Stream.of(1, 2, 3, 4, 5).count());
        assertEquals(0, Stream.empty().count());
    }

    @Test
    public void testMap() {
        Stream<String> s = Stream.of(1, 2, 3)
                .map(n -> "hello: " + n);
        assertEquals(List.of("hello: 1", "hello: 2", "hello: 3"), s.toList());
    }

    @Test
    public void testFilter() {
        Stream<Integer> s1 = Stream.of(1, 2, 3, 4, 5, 6, 7, 8, 9, 10);
        Stream<Integer> s2 = s1.filter(n -> n % 2 == 0);
        assertEquals(List.of(2, 4, 6, 8, 10), s2.toList());
        Stream<Integer> s3 = s1.filter(n -> n % 2 == 1);
        assertEquals(List.of(1, 3, 5, 7, 9), s3.toList());
        Stream<Integer> s4 = s1.filter(n -> n > 20);
        assertTrue(s4.end());
        assertTrue(s4.toList().isEmpty());
        Stream<Integer> s5 = Stream.fromGenerator(100, n -> n + 1)
                .filter(n -> n % 3 == 0)
                .limit(5);
        assertEquals(List.of(102, 105, 108, 111, 114), s5.toList());
    }

    @Test
    public void testConcat() {
        assertEquals(List.of(1, 2, 3, 4, 9, 8, 7), Stream.of(1, 2, 3, 4).concat(Stream.of(9, 8, 7)).toList());
        assertEquals(List.of("aaa", "bbb"), Stream.empty().concat(Stream.of("aaa", "bbb")).toList());
        assertEquals(List.of(123, 456), Stream.of(123, 456).concat(Stream.empty()).toList());
        assertEquals(List.of(1, 3, 5), Stream.fromGenerator(1, n -> n + 2).concat(Stream.fromGenerator(2, n -> n + 2)).limit(3).toList());
        assertTrue(Stream.empty().concat(Stream.empty()).end());
        assertTrue(Stream.empty().concat(Stream.empty()).toList().isEmpty());
    }

    @Test
    public void testInterleave() {
        assertEquals(List.of(1, 2, 3, 4, 5, 6, 7, 8), Stream.of(1, 3, 5, 7).interleave(Stream.of(2, 4, 6, 8)).toList());
        assertEquals(List.of(2, 1, 5, 3, 4), Stream.of(2, 5).interleave(Stream.of(1, 3, 4)).toList());
        assertEquals(List.of(3, 2, 8, 4, 5), Stream.of(3, 8, 5).interleave(Stream.of(2, 4)).toList());
        assertEquals(List.of(1, 2, 3, 4), Stream.fromGenerator(1, n -> n + 2).interleave(Stream.fromGenerator(2, n -> n + 2)).limit(4).toList());
        assertEquals(List.of(1, 2, 3), Stream.empty().interleave(Stream.of(1, 2, 3)).toList());
        assertEquals(List.of(1, 2, 3), Stream.of(1, 2, 3).interleave(Stream.empty()).toList());
    }

    @Test
    public void testFlatMap() {
        Stream<Integer> s1 = Stream.of(10, 20, 30)
                .flatMap(n -> Stream.of(n + 1, n + 2, n + 3));
        assertEquals(List.of(11, 12, 13, 21, 22, 23, 31, 32, 33), s1.toList());
        Stream<String> s2 = Stream.of(1, 2, 3)
                .flatMap(n -> Stream.of(n + "a", n + "b"));
        assertEquals(List.of("1a", "1b", "2a", "2b", "3a", "3b"), s2.toList());
    }
}
