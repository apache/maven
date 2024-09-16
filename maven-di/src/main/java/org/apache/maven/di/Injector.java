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
package org.apache.maven.di;

import java.lang.annotation.Annotation;
import java.util.function.Supplier;

import org.apache.maven.api.annotations.Nonnull;
import org.apache.maven.di.impl.InjectorImpl;

public interface Injector {

    //
    // Builder API
    //

    @Nonnull
    static Injector create() {
        return new InjectorImpl();
    }

    @Nonnull
    Injector discover(@Nonnull ClassLoader classLoader);

    @Nonnull
    Injector bindScope(@Nonnull Class<? extends Annotation> scopeAnnotation, @Nonnull Scope scope);

    @Nonnull
    Injector bindScope(@Nonnull Class<? extends Annotation> scopeAnnotation, @Nonnull Supplier<Scope> scope);

    @Nonnull
    Injector bindImplicit(@Nonnull Class<?> cls);

    @Nonnull
    <T> Injector bindInstance(@Nonnull Class<T> cls, @Nonnull T instance);

    //
    // Bean access
    //

    <T> void injectInstance(@Nonnull T instance);

    @Nonnull
    <T> T getInstance(@Nonnull Class<T> key);

    @Nonnull
    <T> T getInstance(@Nonnull Key<T> key);
}
