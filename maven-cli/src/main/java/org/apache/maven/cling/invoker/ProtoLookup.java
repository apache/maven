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
package org.apache.maven.cling.invoker;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import org.apache.maven.api.services.Lookup;
import org.apache.maven.api.services.LookupException;

import static java.util.Objects.requireNonNull;

/**
 * Proto-{@link Lookup} offer ways to provide early components to invoker.
 */
public class ProtoLookup implements Lookup {
    private final Map<Class<?>, Object> components;

    private ProtoLookup(Map<Class<?>, Object> components) {
        this.components = components;
    }

    @Override
    public <T> T lookup(Class<T> type) {
        Optional<T> optional = lookupOptional(type);
        if (optional.isPresent()) {
            return optional.get();
        } else {
            throw new LookupException("No mapping for key: " + type.getName());
        }
    }

    @Override
    public <T> T lookup(Class<T> type, String name) {
        return lookup(type);
    }

    @Override
    public <T> Optional<T> lookupOptional(Class<T> type) {
        return Optional.of(type.cast(components.get(type)));
    }

    @Override
    public <T> Optional<T> lookupOptional(Class<T> type, String name) {
        return lookupOptional(type);
    }

    @Override
    public <T> List<T> lookupList(Class<T> type) {
        return List.of();
    }

    @Override
    public <T> Map<String, T> lookupMap(Class<T> type) {
        return Map.of();
    }

    public static Builder builder() {
        return new Builder();
    }

    public static class Builder {
        private final Map<Class<?>, Object> components = new HashMap<>();

        public ProtoLookup build() {
            return new ProtoLookup(components);
        }

        public <T> Builder addMapping(Class<T> type, T component) {
            requireNonNull(type, "type");
            requireNonNull(component, "component");
            if (components.put(type, component) != null) {
                throw new IllegalStateException("Duplicate mapping for type: " + type.getName());
            }
            return this;
        }
    }
}
