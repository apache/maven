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
 * Was intended to indicate that the annotated bean has a lifespan limited to a given
 * mojo execution, but this annotation is unfinished and has no effect.
 *
 * @deprecated Unfinished — do not use. The Guice/Sisu binding for this scope is
 *     commented out in {@code MojoExecutionScopeModule}, so the annotation has no
 *     effect. Additionally, unlike {@link SessionScoped}, the mojo execution scope
 *     does not create a proxy when a scoped component is injected into a wider scope,
 *     causing a {@code DIException} at startup.
 *     <p>
 *     For v4 plugins, no scope annotation is needed: {@code DefaultMavenPluginManager}
 *     creates a fresh injector per mojo execution and binds {@code Session},
 *     {@code Project}, {@code MojoExecution}, and {@code Log} directly via
 *     {@code bindInstance}, so plain {@code @Inject} is sufficient.
 *     <p>
 *     For legacy extension components in the shared Plexus/Sisu container, use
 *     {@code org.apache.maven.execution.scope.MojoExecutionScoped} instead.
 *
 * @since 4.0.0
 */
@Deprecated(since = "4.0.0", forRemoval = true)
@Scope
@Documented
@Retention(RUNTIME)
@Target({TYPE, METHOD})
public @interface MojoExecutionScoped {}
