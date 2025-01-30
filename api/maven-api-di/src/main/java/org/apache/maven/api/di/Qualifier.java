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

import static java.lang.annotation.ElementType.ANNOTATION_TYPE;
import static java.lang.annotation.RetentionPolicy.RUNTIME;

/**
 * Meta-annotation that marks other annotations as qualifier annotations.
 * <p>
 * Qualifiers are used to distinguish between multiple beans of the same type,
 * allowing for more precise control over which implementation should be injected.
 * Custom qualifier annotations should be annotated with {@code @Qualifier}.
 * <p>
 * Example of creating a custom qualifier:
 * <pre>
 * {@literal @}Qualifier
 * {@literal @}Retention(RUNTIME)
 * public @interface Database {
 *     String value();
 * }
 * </pre>
 *
 * @see Named
 * @since 4.0.0
 */
@Target(ANNOTATION_TYPE)
@Retention(RUNTIME)
@Documented
public @interface Qualifier {}
