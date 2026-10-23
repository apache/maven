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

import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Supplier;

import org.apache.maven.api.SessionData;

public final class MapBackedSessionData implements SessionData {
    private final ConcurrentHashMap<Key<?>, Object> map = new ConcurrentHashMap<>();

    @Override
    public <T> void set(Key<T> key, T value) {
        Objects.requireNonNull(key, "key");
        if (value == null) {
            map.remove(key);
        } else {
            map.put(key, value);
        }
    }

    @Override
    public <T> boolean replace(Key<T> key, T oldValue, T newValue) {
        Objects.requireNonNull(key, "key");
        if (newValue == null) {
            return map.remove(key, oldValue);
        }
        return map.replace(key, oldValue, newValue);
    }

    @SuppressWarnings("unchecked")
    @Override
    public <T> T get(Key<T> key) {
        Objects.requireNonNull(key, "key");
        return (T) map.get(key);
    }

    @SuppressWarnings("unchecked")
    @Override
    public <T> T computeIfAbsent(Key<T> key, Supplier<T> supplier) {
        Objects.requireNonNull(key, "key");
        Objects.requireNonNull(supplier, "supplier");
        return (T) map.computeIfAbsent(key, ignored -> supplier.get());
    }
}
