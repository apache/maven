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

/**
 * Maven 4 API for class loading realms and isolation.
 * <p>
 * This package provides the public API for Maven's class loading system, which allows
 * for isolated class loading environments (realms) with controlled imports and exports
 * between them.
 * </p>
 * <p>
 * Key concepts:
 * </p>
 * <ul>
 * <li>{@link org.apache.maven.api.classworlds.ClassWorld} - A container for multiple class realms</li>
 * <li>{@link org.apache.maven.api.classworlds.ClassRealm} - An isolated class loading environment</li>
 * <li>{@link org.apache.maven.api.classworlds.Strategy} - Defines class loading delegation behavior</li>
 * <li>{@link org.apache.maven.api.classworlds.ClassWorldListener} - Listens to realm lifecycle events</li>
 * </ul>
 * <p>
 * This API follows Maven 4 conventions:
 * </p>
 * <ul>
 * <li>Only public interfaces and enums are exposed</li>
 * <li>All interfaces are marked as {@code @Experimental}</li>
 * <li>Proper nullability annotations are used</li>
 * <li>Implementation details are hidden behind the API</li>
 * </ul>
 *
 * @since 4.1.0
 */
@org.apache.maven.api.annotations.Experimental
package org.apache.maven.api.classworlds;
