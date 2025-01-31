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

import static java.lang.annotation.RetentionPolicy.RUNTIME;

/**
 * Denotes that a bean should be created as a singleton instance.
 * <p>
 * Singleton-scoped beans are instantiated once and reused throughout the entire
 * Maven execution. This scope should be used for stateless services or components
 * that can be safely shared across the entire build process.
 * <p>
 * Example usage:
 * <pre>
 * {@literal @}Singleton
 * public class GlobalConfiguration {
 *     // Implementation
 * }
 * </pre>
 *
 * @see Scope
 * @since 4.0.0
 */
@Scope
@Documented
@Retention(RUNTIME)
public @interface Singleton {}
