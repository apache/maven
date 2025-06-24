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

/**
 * The main entry point for Maven's dependency injection framework.
 * <p>
 * The Injector manages the creation and injection of objects within the Maven build process.
 * It provides both a builder API for configuring the injection behavior and methods for
 * accessing and injecting beans.
 * <p>
 * Example usage:
 * <pre>
 * Injector injector = Injector.create()
 *     .discover(getClass().getClassLoader())
 *     .bindInstance(Configuration.class, config);
 *
 * MyService service = injector.getInstance(MyService.class);
 * </pre>
 *
 * @since 4.0.0
 */
public interface Injector {

    /**
     * Creates a new Injector instance with default settings.
     *
     * @return a new Injector instance
     */
    @Nonnull
    static Injector create() {
        return new InjectorImpl();
    }

    /**
     * Configures the injector to discover injectable components from the specified ClassLoader.
     * <p>
     * This method scans for classes annotated with injection-related annotations and
     * automatically registers them with the injector.
     *
     * @param classLoader the ClassLoader to scan for injectable components
     * @return this injector instance for method chaining
     * @throws NullPointerException if classLoader is null
     */
    @Nonnull
    Injector discover(@Nonnull ClassLoader classLoader);

    /**
     * Binds a scope annotation to its implementation.
     * <p>
     * This allows custom scopes to be registered with the injector. The scope annotation
     * must be annotated with {@link org.apache.maven.api.di.Scope}.
     *
     * @param scopeAnnotation the annotation class that defines the scope
     * @param scope the scope implementation
     * @return this injector instance for method chaining
     * @throws NullPointerException if either parameter is null
     */
    @Nonnull
    Injector bindScope(@Nonnull Class<? extends Annotation> scopeAnnotation, @Nonnull Scope scope);

    /**
     * Binds a scope annotation to a supplier that creates scope implementations.
     * <p>
     * Similar to {@link #bindScope(Class, Scope)} but allows lazy creation of scope
     * implementations.
     *
     * @param scopeAnnotation the annotation class that defines the scope
     * @param scope supplier that creates scope implementations
     * @return this injector instance for method chaining
     * @throws NullPointerException if either parameter is null
     */
    @Nonnull
    Injector bindScope(@Nonnull Class<? extends Annotation> scopeAnnotation, @Nonnull Supplier<Scope> scope);

    /**
     * Registers a class for implicit binding.
     * <p>
     * Implicit bindings allow the injector to create instances of classes without
     * explicit binding definitions. The class must have appropriate injection annotations.
     *
     * @param cls the class to register for implicit binding
     * @return this injector instance for method chaining
     * @throws NullPointerException if cls is null
     */
    @Nonnull
    Injector bindImplicit(@Nonnull Class<?> cls);

    /**
     * Binds a specific instance to a class type.
     * <p>
     * This method allows pre-created instances to be used for injection instead of
     * having the injector create new instances.
     *
     * @param <T> the type of the instance
     * @param cls the class to bind to
     * @param instance the instance to use for injection
     * @return this injector instance for method chaining
     * @throws NullPointerException if either parameter is null
     */
    @Nonnull
    <T> Injector bindInstance(@Nonnull Class<T> cls, @Nonnull T instance);

    /**
     * Performs field and method injection on an existing instance.
     * <p>
     * This method will inject dependencies into annotated fields and methods of
     * the provided instance but will not create a new instance.
     *
     * @param <T> the type of the instance
     * @param instance the instance to inject dependencies into
     * @throws NullPointerException if instance is null
     */
    <T> void injectInstance(@Nonnull T instance);

    /**
     * Retrieves or creates an instance of the specified type.
     *
     * @param <T> the type to retrieve
     * @param key the class representing the type to retrieve
     * @return an instance of the requested type
     * @throws NullPointerException if key is null
     * @throws IllegalStateException if the type cannot be provided
     */
    @Nonnull
    <T> T getInstance(@Nonnull Class<T> key);

    /**
     * Retrieves or creates an instance for the specified key.
     * <p>
     * This method allows retrieval of instances with specific qualifiers or
     * generic type parameters.
     *
     * @param <T> the type to retrieve
     * @param key the key identifying the instance to retrieve
     * @return an instance matching the requested key
     * @throws NullPointerException if key is null
     * @throws IllegalStateException if the type cannot be provided
     */
    @Nonnull
    <T> T getInstance(@Nonnull Key<T> key);
}
