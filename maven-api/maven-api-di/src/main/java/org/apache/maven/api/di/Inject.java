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

import static java.lang.annotation.ElementType.CONSTRUCTOR;
import static java.lang.annotation.ElementType.FIELD;
import static java.lang.annotation.ElementType.METHOD;
import static java.lang.annotation.RetentionPolicy.RUNTIME;

/**
 * Marks a dependency injection point for constructor, method, or field injection.
 * <p>
 * This annotation is used to identify injection points where the container should
 * provide an instance of the requested type. It can be applied to:
 * <ul>
 *   <li>Constructors</li>
 *   <li>Methods</li>
 *   <li>Fields</li>
 * </ul>
 * <p>
 * Example usage:
 * <pre>
 * public class MyService {
 *     private final Repository repository;
 *
 *     {@literal @}Inject
 *     public MyService(Repository repository) {
 *         this.repository = repository;
 *     }
 * }
 * </pre>
 *
 * @see Named
 * @since 4.0.0
 */
@Target({FIELD, CONSTRUCTOR, METHOD})
@Retention(RUNTIME)
@Documented
public @interface Inject {}
