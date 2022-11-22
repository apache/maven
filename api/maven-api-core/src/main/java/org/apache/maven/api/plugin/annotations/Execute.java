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

/**
 * Used if your Mojo needs to fork a <a href="/ref/3.0.4/maven-core/lifecycles.html">lifecycle</a>.
 *
 * @since 4.0
 */
@Experimental
@Documented
@Retention(RetentionPolicy.CLASS)
@Target(ElementType.TYPE)
@Inherited
public @interface Execute {
    /**
     * Lifecycle phase to fork. Note that specifying a phase overrides specifying a goal.
     * For custom lifecycle phase ids use {@link #customPhase()} instead.
     * Only one of {@link #customPhase()} and {@link #phase()} must be set.
     * @return the phase
     */
    LifecyclePhase phase() default LifecyclePhase.NONE;

    /**
     * Custom lifecycle phase to fork. Note that specifying a phase overrides specifying a goal.
     * This element should only be used for non-standard phases. For standard phases rather use {@link #phase()}.
     * Only one of {@link #customPhase()} and {@link #phase()} must be set.
     * @return the custom phase id
     */
    String customPhase() default "";

    /**
     * Goal to fork. Note that specifying a phase overrides specifying a goal. The specified <code>goal</code> must be
     * another goal of the same plugin.
     * @return the goal
     */
    String goal() default "";

    /**
     * Lifecycle id of the lifecycle that defines {@link #phase()}. Only valid in combination with {@link #phase()}. If
     * not specified, Maven will use the lifecycle of the current build.
     *
     * @see <a href="https://maven.apache.org/maven-plugin-api/lifecycle-mappings.html">Lifecycle Mappings</a>
     * @return the lifecycle id
     */
    String lifecycle() default "";
}
