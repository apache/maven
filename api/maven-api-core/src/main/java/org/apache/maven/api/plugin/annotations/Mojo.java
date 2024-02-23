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
 * This annotation will mark your class as a Mojo (ie. goal in a Maven plugin).
 * The mojo can be annotated with {@code jakarta.inject.*} annotations.
 * The {@link Parameter} annotation can be added on fields to inject data
 * from the plugin configuration or from other components.
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
    LifecyclePhase defaultPhase() default LifecyclePhase.NONE;

    /**
     * does your mojo requires a project to be executed?
     * @return requires a project
     */
    boolean projectRequired() default true;

    /**
     * if the Mojo uses the Maven project and its child modules.
     * @return uses the Maven project and its child modules
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
}
