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
 * Provides tools for processing Maven dependency injection annotations at compile time.
 * <p>
 * This package contains annotation processors that generate metadata files used by
 * the Maven dependency injection system. The main component is the {@link org.apache.maven.di.tool.DiIndexProcessor},
 * which processes classes annotated with {@link org.apache.maven.api.di.Named} and creates an index file
 * that allows for efficient discovery of injectable components at runtime.
 * <p>
 * The generated index is stored at {@code META-INF/maven/org.apache.maven.api.di.Inject} and contains
 * the fully qualified names of all classes annotated with {@code @Named}.
 *
 * @since 4.0.0
 */
package org.apache.maven.di.tool;
