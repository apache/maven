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
package org.apache.maven.api.build.incremental;

import java.lang.annotation.Retention;
import java.lang.annotation.Target;

import org.apache.maven.api.annotations.Experimental;

import static java.lang.annotation.ElementType.FIELD;
import static java.lang.annotation.ElementType.METHOD;
import static java.lang.annotation.ElementType.PARAMETER;
import static java.lang.annotation.ElementType.TYPE;
import static java.lang.annotation.RetentionPolicy.RUNTIME;

/**
 * Optional annotation that customizes how the incremental build implementation handles
 * configuration parameters for change detection.
 *
 * <p>When a mojo is re-executed, the build context compares the current configuration
 * parameter values against those from the previous build. If any tracked parameter has
 * changed, all inputs are treated as modified, forcing a full rebuild. This annotation
 * controls which parameters participate in that comparison.</p>
 *
 * <p>By default, all mojo parameters are considered. Use {@code @Incremental(consider = false)}
 * to exclude parameters that do not affect the output (e.g., logging verbosity, thread count).
 * This annotation is <strong>mandatory</strong> on {@link org.apache.maven.api.Project}
 * and {@link org.apache.maven.api.Session} attributes to explicitly indicate whether
 * they should be considered:</p>
 *
 * <pre>{@code
 * @Parameter(defaultValue = "${project}", readonly = true)
 * @Incremental(consider = false)
 * private Project project;
 *
 * @Parameter(property = "outputDirectory", required = true)
 * @Incremental  // considered by default
 * private Path outputDirectory;
 * }</pre>
 *
 * @since 4.1.0
 * @see IncrementalContext
 */
@Experimental
@Retention(RUNTIME)
@Target({FIELD, METHOD, PARAMETER, TYPE})
public @interface Incremental {

    /**
     * {@return whether to consider (the default) or ignore the annotated configuration parameter}
     */
    boolean consider() default true;
}
