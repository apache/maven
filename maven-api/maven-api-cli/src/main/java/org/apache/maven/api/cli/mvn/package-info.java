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
 * Provides the API for the Maven build tool ({@code mvn}).
 *
 * <p>This package contains interfaces and classes specific to the main Maven build
 * tool, which is responsible for project build lifecycle execution and dependency management.</p>
 *
 * <p>Key features include:</p>
 * <ul>
 *   <li>Build lifecycle execution control</li>
 *   <li>Project-specific configuration</li>
 *   <li>Goal execution and phase mapping</li>
 *   <li>Multi-module build coordination</li>
 * </ul>
 *
 * @see org.apache.maven.api.cli.Tools#MVN_CMD
 * @see org.apache.maven.api.cli.Tools#MVN_NAME
 * @since 4.0.0
 */
package org.apache.maven.api.cli.mvn;
