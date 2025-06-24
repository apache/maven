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
import static java.lang.annotation.RetentionPolicy.RUNTIME;

/**
 * Marks a method as a provider of beans for dependency injection.
 * <p>
 * This annotation can be used on static methods to programmatically create and configure
 * beans that will be managed by the dependency injection container. It's particularly
 * useful when the bean creation requires complex logic or when the bean needs to be
 * configured based on runtime conditions.
 * <p>
 * Example usage:
 * <pre>
 * public class Providers {
 *     {@literal @}Provides
 *     {@literal @}Singleton
 *     public static Configuration provideConfiguration() {
 *         return Configuration.load();
 *     }
 * }
 * </pre>
 *
 * @since 4.0.0
 */
@Target(METHOD)
@Retention(RUNTIME)
@Documented
public @interface Provides {}
