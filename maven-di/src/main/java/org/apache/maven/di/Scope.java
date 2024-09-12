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
 * A {@code Scope} defines how visible instances are when managed by a {@link org.apache.maven.di.Injector}.
 * Typically, instances are created with <i>no scope</i>, meaning they don’t retain any state from the
 * framework’s perspective: the {@code Injector} generates the instance, injects it into the necessary class,
 * and then immediately forgets it. By linking a scope to a specific binding, the created instance can be
 * “remembered” and reused for future injections.
 * <p>
 * Instances are associated to a given scope by means of a {@link org.apache.maven.api.di.Scope @Scope}
 * annotation, usually put on another annotation. For example, the {@code @Singleton} annotation is used
 * to indicate that a given binding should be scoped as a singleton.
 * <p>
 * The following scopes are currently supported:
 * <ul>
 *     <li>{@link org.apache.maven.api.di.Singleton @Singleton}</li>
 *     <li>{@link org.apache.maven.api.di.SessionScoped @SessionScoped}</li>
 *     <li>{@link org.apache.maven.api.di.MojoExecutionScoped @MojoExecutionScoped}</li>
 * </ul>
 *
 * @since 4.0.0
 */
@Experimental
public interface Scope {

    @Nonnull
    <T> Supplier<T> scope(@Nonnull Key<T> key, @Nonnull Supplier<T> unscoped);
}
