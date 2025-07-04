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
package org.apache.maven.internal.xml;

import java.io.Serializable;
import java.util.AbstractMap;
import java.util.AbstractSet;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.Set;
import java.util.function.BiFunction;
import java.util.function.Function;
import java.util.function.Predicate;

/**
 * This should be removed when https://bugs.openjdk.org/browse/JDK-8323729
 * is released in our minimum JDK.
 */
class ImmutableCollections {

    private static final Map<?, ?> EMPTY_MAP = new AbstractImmutableMap<Object, Object>() {
        @Override
        public Set<Entry<Object, Object>> entrySet() {
            return new AbstractImmutableSet<Entry<Object, Object>>() {
                @Override
                public Iterator<Entry<Object, Object>> iterator() {
                    return new Iterator<Entry<Object, Object>>() {
                        @Override
                        public boolean hasNext() {
                            return false;
                        }

                        @Override
                        public Entry<Object, Object> next() {
                            throw new NoSuchElementException();
                        }
                    };
                }

                @Override
                public int size() {
                    return 0;
                }
            };
        }
    };

    static <E1, E2 extends E1> List<E1> copy(Collection<E2> collection) {
        return collection == null ? List.of() : List.copyOf(collection);
    }

    static <K, V> Map<K, V> copy(Map<K, V> map) {
        if (map == null) {
            return emptyMap();
        } else if (map instanceof AbstractImmutableMap) {
            return map;
        } else {
            switch (map.size()) {
                case 0:
                    return emptyMap();
                case 1:
                    Map.Entry<K, V> entry = map.entrySet().iterator().next();
                    return singletonMap(entry.getKey(), entry.getValue());
                default:
                    return new MapN<>(map);
            }
        }
    }

    @SuppressWarnings("unchecked")
    static <K, V> Map<K, V> emptyMap() {
        return (Map<K, V>) EMPTY_MAP;
    }

    static <K, V> Map<K, V> singletonMap(K key, V value) {
        return new Map1<>(key, value);
    }

    private static class Map1<K, V> extends AbstractImmutableMap<K, V> {
        private final Entry<K, V> entry;

        private Map1(K key, V value) {
            this.entry = new SimpleImmutableEntry<>(key, value);
        }

        @Override
        public Set<Entry<K, V>> entrySet() {
            return new AbstractImmutableSet<Entry<K, V>>() {
                @Override
                public Iterator<Entry<K, V>> iterator() {
                    return new Iterator<Entry<K, V>>() {
                        int index = 0;

                        @Override
                        public boolean hasNext() {
                            return index == 0;
                        }

                        @Override
                        public Entry<K, V> next() {
                            if (index++ == 0) {
                                return entry;
                            }
                            throw new NoSuchElementException();
                        }
                    };
                }

                @Override
                public int size() {
                    return 1;
                }
            };
        }
    }

    private static class MapN<K, V> extends AbstractImmutableMap<K, V> {
        private final Object[] entries;

        private MapN(Map<K, V> map) {
            if (map != null) {
                entries = new Object[map.size()];
                int idx = 0;
                for (Entry<K, V> e : map.entrySet()) {
                    entries[idx++] = new SimpleImmutableEntry<>(e.getKey(), e.getValue());
                }
            } else {
                entries = new Object[0];
            }
        }

        @Override
        public Set<Entry<K, V>> entrySet() {
            return new AbstractImmutableSet<Entry<K, V>>() {
                @Override
                public Iterator<Entry<K, V>> iterator() {
                    return new Iterator<Entry<K, V>>() {
                        int index = 0;

                        @Override
                        public boolean hasNext() {
                            return index < entries.length;
                        }

                        @SuppressWarnings("unchecked")
                        @Override
                        public Entry<K, V> next() {
                            if (index < entries.length) {
                                return (Entry<K, V>) entries[index++];
                            }
                            throw new NoSuchElementException();
                        }
                    };
                }

                @Override
                public int size() {
                    return entries.length;
                }
            };
        }
    }

    private abstract static class AbstractImmutableMap<K, V> extends AbstractMap<K, V> implements Serializable {
        @Override
        public void replaceAll(BiFunction<? super K, ? super V, ? extends V> function) {
            throw uoe();
        }

        @Override
        public V putIfAbsent(K key, V value) {
            throw uoe();
        }

        @Override
        public boolean remove(Object key, Object value) {
            throw uoe();
        }

        @Override
        public boolean replace(K key, V oldValue, V newValue) {
            throw uoe();
        }

        @Override
        public V replace(K key, V value) {
            throw uoe();
        }

        @Override
        public V computeIfAbsent(K key, Function<? super K, ? extends V> mappingFunction) {
            throw uoe();
        }

        @Override
        public V computeIfPresent(K key, BiFunction<? super K, ? super V, ? extends V> remappingFunction) {
            throw uoe();
        }

        @Override
        public V compute(K key, BiFunction<? super K, ? super V, ? extends V> remappingFunction) {
            throw uoe();
        }

        @Override
        public V merge(K key, V value, BiFunction<? super V, ? super V, ? extends V> remappingFunction) {
            throw uoe();
        }
    }

    private abstract static class AbstractImmutableSet<E> extends AbstractSet<E> implements Serializable {
        @Override
        public boolean removeAll(Collection<?> c) {
            throw uoe();
        }

        @Override
        public boolean add(E e) {
            throw uoe();
        }

        @Override
        public boolean remove(Object o) {
            throw uoe();
        }

        @Override
        public boolean retainAll(Collection<?> c) {
            throw uoe();
        }

        @Override
        public void clear() {
            throw uoe();
        }

        @Override
        public boolean removeIf(Predicate<? super E> filter) {
            throw uoe();
        }
    }

    private static UnsupportedOperationException uoe() {
        return new UnsupportedOperationException();
    }
}
