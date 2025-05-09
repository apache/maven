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

import java.util.function.Supplier;

import org.apache.maven.api.annotations.Experimental;
import org.apache.maven.api.annotations.Nonnull;

/**
 * Defines how object instances are managed within a specific scope.
 * <p>
 * A Scope controls the lifecycle and visibility of objects created by the injector.
 * It determines when new instances are created and when existing instances can be
 * reused. This allows for different caching strategies depending on the desired
 * lifecycle of the objects.
 * <p>
 * Example implementation for a simple caching scope:
 * <pre>
 * public class CachingScope implements Scope {
 *     private final Map&lt;Key&lt;?&gt;, Object&gt; cache = new ConcurrentHashMap&lt;&gt;();
 *
 *     {@literal @}Override
 *     public &lt;T&gt; Supplier&lt;T&gt; scope(Key&lt;T&gt; key, Supplier&lt;T&gt; unscoped) {
 *         return () -&gt; {
 *             return (T) cache.computeIfAbsent(key, k -&gt; unscoped.get());
 *         };
 *     }
 * }
 * </pre>
 *
 * @see org.apache.maven.api.di.Scope
 * @since 4.0.0
 */
@Experimental
public interface Scope {

    /**
     * Scopes a supplier of instances.
     * <p>
     * This method wraps an unscoped instance supplier with scope-specific logic
     * that controls when new instances are created versus when existing instances
     * are reused.
     *
     * @param <T> the type of instance being scoped
     * @param key the key identifying the instance type
     * @param unscoped the original unscoped instance supplier
     * @return a scoped supplier that implements the scope's caching strategy
     * @throws NullPointerException if key or unscoped is null
     */
    @Nonnull
    <T> Supplier<T> scope(@Nonnull Key<T> key, @Nonnull Supplier<T> unscoped);
}
