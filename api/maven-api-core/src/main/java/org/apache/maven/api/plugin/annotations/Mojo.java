package org.apache.maven.api.plugin.annotations;

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

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Inherited;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

import org.apache.maven.api.ResolutionScope;
import org.apache.maven.api.annotations.Experimental;

/**
 * This annotation will mark your class as a Mojo (ie. goal in a Maven plugin).
 *
 * @since 4.0
 */
@Experimental
@Documented
@Retention( RetentionPolicy.CLASS )
@Target( ElementType.TYPE )
@Inherited
public @interface Mojo
{
    /**
     * goal name (required).
     * @return the goal name
     */
    String name();

    /**
     * default phase to bind your mojo.
     * @return the default phase
     */
    LifecyclePhase defaultPhase() default LifecyclePhase.NONE;

    /**
     * the required dependency resolution scope.
     * @return the required dependency resolution scope
     */
    ResolutionScope requiresDependencyResolution() default ResolutionScope.NONE;

    /**
     * the required dependency collection scope.
     * @return the required dependency collection scope 
     */
    ResolutionScope requiresDependencyCollection() default ResolutionScope.NONE;

    /**
     * your Mojo instantiation strategy. (Only <code>per-lookup</code> and <code>singleton</code> are supported)
     * @return the instantiation strategy
     */
    InstantiationStrategy instantiationStrategy() default InstantiationStrategy.PER_LOOKUP;

    /**
     * does your mojo requires a project to be executed?
     * @return requires a project
     */
    boolean requiresProject() default true;

    /**
     * if the Mojo uses the Maven project and its child modules.
     * @return uses the Maven project and its child modules
     */
    boolean aggregator() default false;

    /**
     * does this Mojo need to be online to be executed?
     * @return need to be online
     */
    boolean requiresOnline() default false;

    /**
     * configurator bean name.
     * @return the configurator bean name
     */
    String configurator() default "";

}
