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
package org.apache.maven.internal.impl;

import java.util.AbstractCollection;
import java.util.Collection;
import java.util.Iterator;
import java.util.function.Function;

public class MappedCollection<U, V> extends AbstractCollection<U> {
    private final Collection<V> list;
    private final Function<V, U> mapper;

    public MappedCollection(Collection<V> list, Function<V, U> mapper) {
        this.list = list;
        this.mapper = mapper;
    }

    @Override
    public Iterator<U> iterator() {
        Iterator<V> it = list.iterator();
        return new Iterator<U>() {
            @Override
            public boolean hasNext() {
                return it.hasNext();
            }

            @Override
            public U next() {
                return mapper.apply(it.next());
            }
        };
    }

    @Override
    public int size() {
        return list.size();
    }
}
