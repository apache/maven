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
 * Specifies the priority of a bean implementation when multiple implementations
 * of the same type are available.
 * <p>
 * Higher values indicate higher priority. When multiple implementations of the same
 * type exist, the one with the highest priority will be selected for injection.
 * <p>
 * Example usage:
 * <pre>
 * {@literal @}Priority(100)
 * public class PreferredImplementation implements Service {
 *     // Implementation
 * }
 * </pre>
 *
 * @since 4.0.0
 */
@Target({TYPE, METHOD})
@Retention(RUNTIME)
@Documented
public @interface Priority {
    /**
     * The priority value for the annotated element.
     * <p>
     * Higher values indicate higher priority. When multiple implementations
     * of the same type exist in the container, the one with the highest
     * priority value will be selected for injection.
     * <p>
     * There are no predefined minimum or maximum values, but it's recommended
     * to use values that allow for future adjustments (e.g., using values
     * like 100, 200, 300 rather than consecutive numbers).
     *
     * @return the priority value for ordering
     */
    int value();
}
