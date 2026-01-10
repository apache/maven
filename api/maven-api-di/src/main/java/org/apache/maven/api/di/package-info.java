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
 * A dependency injection framework for Maven that provides JSR-330 style annotations
 * for managing object lifecycle and dependencies within Maven's build process.
 * <p>
 * This package provides a set of annotations that control how objects are created,
 * managed and injected throughout Maven's execution lifecycle. The framework is designed
 * to be lightweight yet powerful, supporting various scopes of object lifecycle from
 * singleton instances to mojo-execution-scoped beans.
 * <p>
 * Key features include:
 * <ul>
 *   <li>Constructor, method, and field injection</li>
 *   <li>Qualifiers for distinguishing between beans of the same type</li>
 *   <li>Multiple scopes (Singleton, Session, and MojoExecution)</li>
 *   <li>Priority-based implementation selection</li>
 *   <li>Type-safe dependency injection</li>
 * </ul>
 *
 * @since 4.0.0
 */
package org.apache.maven.api.di;
