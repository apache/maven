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
package org.apache.maven.lifecycle.internal.stub;

import java.util.AbstractMap;
import java.util.AbstractSet;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Read-only map that behaves like the {@code Map<String, T>} Sisu injects: its entries are all visible beans in rank
 * order, so one key can appear more than once, and {@link #get(Object)} returns the first (highest-ranked) match.
 *
 * @param <V> the type of the values
 */
public class RankedEntriesMap<V> extends AbstractMap<String, V> {
    private final List<Map.Entry<String, V>> entries = new ArrayList<>();

    /**
     * Adds an entry ranked below all entries added before it.
     */
    public RankedEntriesMap<V> add(String key, V value) {
        entries.add(new SimpleImmutableEntry<>(key, value));
        return this;
    }

    @Override
    public Set<Map.Entry<String, V>> entrySet() {
        return new AbstractSet<Map.Entry<String, V>>() {
            @Override
            public Iterator<Map.Entry<String, V>> iterator() {
                return entries.iterator();
            }

            @Override
            public int size() {
                return entries.size();
            }
        };
    }
}
