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
package org.apache.maven.api.di;

import java.lang.annotation.Documented;
import java.lang.annotation.Retention;
import java.lang.annotation.Target;

import static java.lang.annotation.ElementType.METHOD;
import static java.lang.annotation.ElementType.TYPE;
import static java.lang.annotation.RetentionPolicy.RUNTIME;

/**
 * Indicates that the annotated bean has a lifespan limited to a given mojo execution,
 * which means each mojo execution will result in a different instance being injected.
 * <p>
 * The following objects will be bound to the mojo execution scope:
 *     <ul>
 *         <li>{@code org.apache.maven.api.MojoExecution}</li>
 *         <li>{@code org.apache.maven.api.Project}</li>
 *         <li>{@code org.apache.maven.api.plugin.Log}</li>
 *     </ul>
 *
 * @since 4.0.0
 * @deprecated This annotation is an unfinished counterpart of the Maven 3
 *             {@code org.apache.maven.execution.scope.MojoExecutionScoped}
 *             annotation and should not be used. Its scope is not registered
 *             in the Maven 4 dependency injection container, and it does not
 *             support proxying when injected into components with a wider scope.
 *             The implementation relies on a {@code ThreadLocal} execution
 *             context, which requires an active mojo execution.
 *             <p>
 *             For Maven 4 plugins, use direct {@code @Inject} injection for
 *             execution context objects such as {@code MojoExecution},
 *             {@code Project}, and {@code Log}. Use {@code @SessionScoped}
 *             for session-scoped components. For legacy extensions running
 *             in the shared Plexus/Sisu container, use the Maven 3
 *             {@code org.apache.maven.execution.scope.MojoExecutionScoped}
 *             annotation.
 */
@Deprecated
@Scope
@Documented
@Retention(RUNTIME)
@Target({TYPE, METHOD})
public @interface MojoExecutionScoped {}
