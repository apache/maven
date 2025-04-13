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
 * Provides the API for Maven's command-line interface and tools.
 *
 * <p>This package contains interfaces and classes for:</p>
 * <ul>
 *   <li>Command-line argument parsing and processing</li>
 *   <li>Maven tool invocation ({@code mvn}, {@code mvnenc}, {@code mvnsh})</li>
 *   <li>Core extensions configuration</li>
 *   <li>Early-stage logging before the full Maven logging system is initialized</li>
 * </ul>
 *
 * <p>The main components are:</p>
 * <ul>
 *   <li>{@link org.apache.maven.api.cli.Invoker} - Base interface for executing Maven tools</li>
 *   <li>{@link org.apache.maven.api.cli.Parser} - Processes command-line arguments into invoker requests</li>
 *   <li>{@link org.apache.maven.api.cli.Options} - Represents Maven configuration options</li>
 *   <li>{@link org.apache.maven.api.cli.extensions.CoreExtensions} - Manages Maven core extensions</li>
 * </ul>
 *
 * <p>Core extensions can be configured through {@code .mvn/extensions.xml} in the project base directory
 * to enhance Maven's capabilities during build execution.</p>
 *
 * @since 4.0.0
 */
package org.apache.maven.api.cli;
