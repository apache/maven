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
package org.apache.maven.api.plugin.annotations;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Inherited;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

import org.apache.maven.api.annotations.Experimental;
import org.apache.maven.api.annotations.Nonnull;

/**
 * This annotation will mark your class as a Mojo, which is the implementation of a goal in a Maven plugin.
 * <p>
 * The mojo can be annotated with {@code org.apache.maven.api.di.*} annotations to
 * control the lifecycle of the mojo itself, and to inject other beans.
 * </p>
 * <p>
 * The mojo class can also be injected with an {@link Execute} annotation to specify a
 * forked lifecycle.
 * </p>
 * <p>
 * The {@link Parameter} annotation can be added on fields to inject data
 * from the plugin configuration or from other components.
 * </p>
 * <p>
 * Fields can also be annotated with the {@link Resolution} annotation to be injected
 * with the dependency collection or resolution result for the project.
 * </p>
 *
 * @since 4.0.0
 */
@Experimental
@Documented
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.TYPE)
@Inherited
public @interface Mojo {
    /**
     * goal name (required).
     * @return the goal name
     */
    @Nonnull
    String name();

    /**
     * default phase to bind your mojo.
     * @return the default phase
     */
    @Nonnull
    String defaultPhase() default "";

    /**
     * does your mojo requires a project to be executed?
     * @return requires a project
     */
    boolean projectRequired() default true;

    /**
     * if the Mojo uses the Maven project and its subprojects.
     * @return uses the Maven project and its subprojectss
     */
    boolean aggregator() default false;

    /**
     * does this Mojo need to be online to be executed?
     * @return need to be online
     */
    boolean onlineRequired() default false;

    /**
     * TODO: v4: add a SPI for the configurator
     * configurator bean name.
     * @return the configurator bean name
     */
    @Nonnull
    String configurator() default "";

    /**
     * Indicates whether dependency collection will be
     * required when executing the Mojo.
     * If not set, it will be inferred from the fields
     * annotated with the {@link Resolution} annotation.
     */
    @Nonnull
    boolean dependencyCollection() default false;

    /**
     * Comma separated list of path scopes that will be
     * required for dependency resolution.
     * If not set, it will be inferred from the fields
     * annotated with the {@link Resolution} annotation.
     */
    @Nonnull
    String dependencyResolutionPathScopes() default "";
}
