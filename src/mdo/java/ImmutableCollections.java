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
package ${package};

import java.io.Serializable;
import java.util.AbstractList;
import java.util.AbstractMap;
import java.util.AbstractSet;
import java.util.Collection;
import java.util.Comparator;
import java.util.Iterator;
import java.util.List;
import java.util.ListIterator;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.Properties;
import java.util.RandomAccess;
import java.util.Set;
import java.util.function.BiFunction;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.function.UnaryOperator;

class ImmutableCollections {

    static <E> List<E> copy(Collection<E> collection) {
        if (collection == null) {
            return List.of();
        }
        return List.copyOf(collection);
    }

    static <K, V> Map<K, V> copy(Map<K, V> map) {
        if (map == null) {
            return Map.of();
        }
        return Map.copyOf(map);
    }

    static Properties copy(Properties properties) {
        if (properties instanceof ROProperties) {
            return properties;
        }
        return new ROProperties(properties);
    }

    private static class ROProperties extends Properties {
        private ROProperties(Properties props) {
            super();
            if (props != null) {
                // Do not use super.putAll, as it may delegate to put which throws an UnsupportedOperationException
                for (Map.Entry<Object, Object> e : props.entrySet()) {
                    super.put(e.getKey(), e.getValue());
                }
            }
        }

        @Override
        public Object put(Object key, Object value) {
            throw uoe();
        }

        @Override
        public Object remove(Object key) {
            throw uoe();
        }

        @Override
        public void putAll(Map<?, ?> t) {
            throw uoe();
        }

        @Override
        public void clear() {
            throw uoe();
        }

        @Override
        public void replaceAll(BiFunction<? super Object, ? super Object, ?> function) {
            throw uoe();
        }

        @Override
        public Object putIfAbsent(Object key, Object value) {
            throw uoe();
        }

        @Override
        public boolean remove(Object key, Object value) {
            throw uoe();
        }

        @Override
        public boolean replace(Object key, Object oldValue, Object newValue) {
            throw uoe();
        }

        @Override
        public Object replace(Object key, Object value) {
            throw uoe();
        }

        @Override
        public Object computeIfAbsent(Object key, Function<? super Object, ?> mappingFunction) {
            throw uoe();
        }

        @Override
        public Object computeIfPresent(Object key, BiFunction<? super Object, ? super Object, ?> remappingFunction) {
            throw uoe();
        }

        @Override
        public Object compute(Object key, BiFunction<? super Object, ? super Object, ?> remappingFunction) {
            throw uoe();
        }

        @Override
        public Object merge(Object key, Object value, BiFunction<? super Object, ? super Object, ?> remappingFunction) {
            throw uoe();
        }
    }

    private static UnsupportedOperationException uoe() {
        return new UnsupportedOperationException();
    }
}
