/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */
package org.apache.maven.model.building;

import java.io.BufferedWriter;
import java.io.IOException;

import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.*;
import java.util.concurrent.locks.ReentrantLock;

/**
 * Graph implementation to detect cycles.
 */
public class Graph {

    /** The initial default size of a hash table. */
    public static final int DEFAULT_INITIAL_SIZE = 32;
    /** The default load factor of a hash table. */
    public static final float DEFAULT_LOAD_FACTOR = .75f;

    public static final int DEFAULT_BLOCK_SIZE = 8;

    /**
     * The array of keys.
     */
    private transient Object[] table;
    /**
     * The array of values.
     */
    private transient Object[] blocks;

    private transient int nextBlock;
    private final int blockSize;
    /**
     * The acceptable load factor.
     */
    private final float f;
    /**
     * The current table size.
     */
    private transient int n;
    /**
     * Threshold after which we rehash. It must be the table size times {@link #f}.
     */
    private transient int maxFill;
    /**
     * The mask for wrapping a position counter.
     */
    private transient int mask;
    /**
     * Number of entries in the set.
     */
    private int size;

    private final ReentrantLock lock = new ReentrantLock();

    /**
     * Creates a new hash map with initial expected {@link Graph#DEFAULT_INITIAL_SIZE} entries
     * and {@link Graph#DEFAULT_LOAD_FACTOR} as load factor.
     */
    public Graph() {
        this(DEFAULT_INITIAL_SIZE, DEFAULT_LOAD_FACTOR, DEFAULT_BLOCK_SIZE);
    }

    /**
     * Creates a new graph.
     *
     * <p>The actual table size will be the least power of two greater than <code>expected</code>/<code>f</code>.
     *
     * @param expected the expected number of elements in the hash set.
     * @param f        the load factor.
     */
    public Graph(final int expected, final float f, final int bs) {
        if (f <= 0 || f > 1) {
            throw new IllegalArgumentException("Load factor must be greater than 0 and smaller than or equal to 1");
        }
        if (expected < 0) {
            throw new IllegalArgumentException("The expected number of elements must be nonnegative");
        }
        if (bs <= 1) {
            throw new IllegalArgumentException("Block size must be greater than 1");
        }
        this.f = f;
        n = arraySize(expected, f);
        mask = n - 1;
        maxFill = maxFill(n, f);
        table = new Object[n * 2];
        blocks = new Object[n * bs];
        nextBlock = 0;
        blockSize = bs;
    }

    public void addEdge(String from, String to) throws CycleDetectedException {
        lock.lock();
        try {
            add(from, to);
        } finally {
            lock.unlock();
        }
        List<String> cycle = visitCycle(table, blocks, mask, Collections.singleton(to), new HashMap<>(), new LinkedList<>());
        if (cycle != null) {
            // remove edge which introduced cycle
            throw new CycleDetectedException(
                    "Edge between '" + from + "' and '" + to + "' introduces to cycle in the graph", cycle);
        }
    }

    void add(final String k, final String v) {
        Object[] table = this.table;
        int mask = this.mask;
        int pos = murmurHash3(k.hashCode()) & mask;
        int pos2 = pos << 1;
        while (table[pos2] != null) {
            if ((Objects.equals(table[pos2], k))) {
                int block = (Integer) table[pos2 + 1];
                addToBlock(v, block);
                return;
            }
            pos = (pos + 1) & mask;
            pos2 = pos << 1;
        }
        int block = reserve();
        table[pos2] = k;
        table[pos2 + 1] = block;
        addToBlock(v, block);
        if (++size >= maxFill) {
            rehash(arraySize(size + 1, f) * 2);
        }
    }

    private void addToBlock(String v, int block) {
        for (int j = 0; j < blockSize; j++) {
            if (blocks[block + j] == null) {
                blocks[block + j] = v;
                return;
            } else if (blocks[block + j] instanceof Link) {
                block = ((Link) blocks[block + j]).block;
                j = 0;
            }
        }
        Object last = blocks[block + blockSize - 1];
        int next = reserve();
        blocks[block + blockSize - 1] = new Link(next);
        block = next;
        blocks[block] = last;
        blocks[block + 1] = v;
    }

    Set<String> keySet() {
        return new AbstractSet<String>() {
            @Override
            public Iterator<String> iterator() {
                return new KeySetIterator();
            }

            @Override
            public int size() {
                return size;
            }
        };
    }

    private class KeySetIterator implements Iterator<String> {
        final Object[] table = Graph.this.table;
        int i = 0;

        KeySetIterator() {
            while (i < table.length && table[i] == null) {
                i += 2;
            }
        }

        @Override
        public boolean hasNext() {
            return i < table.length;
        }

        @Override
        public String next() {
            String s = (String) table[i];
            do {
                i += 2;
            } while (i < table.length && table[i] == null);
            return s;
        }
    }

    Iterable<String> get(final Object k) {
        return get(table, blocks, mask, k);
    }

    Iterable<String> get(Object[] table, Object[] blocks, int mask, final Object k) {
        int pos = murmurHash3(k.hashCode()) & mask;
        int pos2 = pos << 1;
        while (table[pos2] != null) {
            if ((k.equals(table[pos2]))) {
                Object v = table[pos2 + 1];
                if (v == null) {
                    return null;
                }
                int block = (Integer) v;
                return () -> new ValuesIterator(blocks, block);
            }
            pos = (pos + 1) & mask;
            pos2 = pos << 1;
        }
        return null;
    }

    private int reserve() {
        if (nextBlock * blockSize >= blocks.length) {
            blocks = Arrays.copyOf(blocks, blocks.length * 4);
        }
        return blockSize * nextBlock++;
    }

    /**
     * Resizes the map.
     *
     * <P>This method implements the basic rehashing strategy, and may be
     * overriden by subclasses implementing different rehashing strategies (e.g.,
     * disk-based rehashing). However, you should not override this method
     * unless you understand the internal workings of this class.
     *
     * @param newN the new size
     */
    protected void rehash(final int newN) {
        int i = 0, pos;
        String k;
        final Object[] table = this.table;
        final int newMask = newN - 1;
        final Object[] newTable = new Object[newN * 2];
        for (int j = size; j-- != 0; ) {
            while (table[i * 2] == null) {
                i++;
            }
            k = (String) table[i * 2];
            pos = murmurHash3((k).hashCode()) & newMask;
            while (newTable[pos * 2] != null) {
                pos = (pos + 1) & newMask;
            }
            newTable[pos * 2] = k;
            newTable[pos * 2 + 1] = table[i * 2 + 1];
            i++;
        }
        n = newN;
        mask = newMask;
        maxFill = maxFill(n, f);
        this.table = newTable;
    }

    /**
     * Avalanches the bits of an integer by applying the finalisation step of MurmurHash3.
     *
     * <p>This function implements the finalisation step of Austin Appleby's <a href="http://sites.google.com/site/murmurhash/">MurmurHash3</a>.
     * Its purpose is to avalanche the bits of the argument to within 0.25% bias. It is used, among other things, to scramble quickly (but deeply) the hash
     * values returned by {@link Object#hashCode()}.
     *
     * @param x an integer.
     * @return a hash value with good avalanching properties.
     */
    @SuppressWarnings("checkstyle:MagicNumber")
    private static int murmurHash3(int x) {
        x ^= x >>> 16;
        x *= 0x85ebca6b;
        x ^= x >>> 13;
        x *= 0xc2b2ae35;
        x ^= x >>> 16;
        return x;
    }

    /** Return the least power of two greater than or equal to the specified value.
     *
     * <p>Note that this function will return 1 when the argument is 0.
     *
     * @param x a long integer smaller than or equal to 2<sup>62</sup>.
     * @return the least power of two greater than or equal to the specified value.
     */
    private static long nextPowerOfTwo(long x) {
        if (x == 0) {
            return 1;
        }
        x--;
        x |= x >> 1;
        x |= x >> 2;
        x |= x >> 4;
        x |= x >> 8;
        x |= x >> 16;
        return (x | x >> 32) + 1;
    }

    /** Returns the maximum number of entries that can be filled before rehashing.
     *
     * @param n the size of the backing array.
     * @param f the load factor.
     * @return the maximum number of entries before rehashing.
     */
    private static int maxFill(final int n, final float f) {
        return (int) Math.ceil(n * f);
    }

    /** Returns the least power of two smaller than or equal to 2<sup>30</sup> and larger than or equal to <code>Math.ceil( expected / f )</code>.
     *
     * @param expected the expected number of elements in a hash table.
     * @param f the load factor.
     * @return the minimum possible size for a backing array.
     * @throws IllegalArgumentException if the necessary size is larger than 2<sup>30</sup>.
     */
    @SuppressWarnings("checkstyle:MagicNumber")
    private static int arraySize(final int expected, final float f) {
        final long s = nextPowerOfTwo((long) Math.ceil(expected / f));
        if (s > (1 << 30)) {
            throw new IllegalArgumentException(
                    "Too large (" + expected + " expected elements with load factor " + f + ")");
        }
        return (int) s;
    }

    public void store() {
        try (BufferedWriter w = Files.newBufferedWriter(Paths.get("dag.txt"))) {
            for (String k : keySet()) {
                w.write(k);
                w.newLine();
                for (String v : get(k)) {
                    w.write("\t" + v);
                    w.newLine();
                }
            }
        } catch (IOException e) {
            // ignore
        }
    }

    static class Link {
        final int block;

        Link(int block) {
            this.block = block;
        }
    }

    private enum DfsState {
        VISITING,
        VISITED
    }

    private List<String> visitCycle(Object[] table, Object[] blocks, int mask,
            Iterable<String> children, Map<String, DfsState> stateMap, LinkedList<String> cycle) {
        if (children != null) {
            for (String v : children) {
                DfsState state = stateMap.putIfAbsent(v, DfsState.VISITING);
                if (state == null) {
                    cycle.addLast(v);
                    List<String> ret = visitCycle(table, blocks, mask, get(table, blocks, mask, v), stateMap, cycle);
                    if (ret != null) {
                        return ret;
                    }
                    cycle.removeLast();
                    stateMap.put(v, DfsState.VISITED);
                } else if (state == DfsState.VISITING) {
                    // we are already visiting this vertex, this mean we have a cycle
                    int pos = cycle.lastIndexOf(v);
                    List<String> ret = cycle.subList(pos, cycle.size());
                    ret.add(v);
                    return ret;
                }
            }
        }
        return null;
    }

    public static class CycleDetectedException extends Exception {
        private final List<String> cycle;

        CycleDetectedException(String message, List<String> cycle) {
            super(message);
            this.cycle = cycle;
        }

        public List<String> getCycle() {
            return cycle;
        }

        @Override
        public String getMessage() {
            return super.getMessage() + " " + String.join(" --> ", cycle);
        }
    }

    private class ValuesIterator implements Iterator<String> {
        private final Object[] blocks;
        private final int block;
        int cur;
        int max;
        String next;

        public ValuesIterator(Object[] blocks, int block) {
            this.blocks = blocks;
            this.block = block;
            cur = block;
            max = cur + blockSize;
            next = adv();
        }

        @Override
        public boolean hasNext() {
            return next != null;
        }

        @Override
        public String next() {
            String n = next;
            cur++;
            next = adv();
            return n;
        }

        private String adv() {
            while (cur < max && cur < blocks.length) {
                Object v = blocks[cur];
                if (v instanceof Link) {
                    cur = ((Link) v).block;
                    max = cur + blockSize;
                } else {
                    return (String) v;
                }
            }
            return null;
        }

    }
}
