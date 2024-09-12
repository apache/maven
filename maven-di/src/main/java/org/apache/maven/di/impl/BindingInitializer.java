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
package org.apache.maven.di.impl;

import java.util.Collection;
import java.util.List;
import java.util.Set;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Supplier;

import static java.util.stream.Collectors.toSet;

public abstract class BindingInitializer<T> {

    private final Set<Dependency<?>> dependencies;

    protected BindingInitializer(Set<Dependency<?>> dependencies) {
        this.dependencies = dependencies;
    }

    public Set<Dependency<?>> getDependencies() {
        return dependencies;
    }

    public abstract Consumer<T> compile(Function<Dependency<?>, Supplier<?>> compiler);

    public static <T> BindingInitializer<T> combine(List<BindingInitializer<T>> bindingInitializers) {
        Set<Dependency<?>> deps = bindingInitializers.stream()
                .map(BindingInitializer::getDependencies)
                .flatMap(Collection::stream)
                .collect(toSet());
        return new BindingInitializer<>(deps) {
            @Override
            public Consumer<T> compile(Function<Dependency<?>, Supplier<?>> compiler) {
                return instance -> bindingInitializers.stream()
                        .map(bindingInitializer -> bindingInitializer.compile(compiler))
                        .forEach(i -> i.accept(instance));
            }
        };
    }
}
