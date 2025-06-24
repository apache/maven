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
 * Maven Immutable POM (Project Object Model) classes, generated from <code>maven.mdo</code> model.
 * <p>
 * This package contains the data model classes that represent the structure of Maven POM files.
 * These classes are immutable to ensure thread safety and prevent unintended modifications.
 * The root class is {@link org.apache.maven.api.model.Model}, which represents the entire POM.
 * <p>
 * Key components include:
 * <ul>
 *   <li>{@link org.apache.maven.api.model.Model} - The root element of a POM file</li>
 *   <li>{@link org.apache.maven.api.model.Dependency} - Represents a project dependency</li>
 *   <li>{@link org.apache.maven.api.model.Plugin} - Represents a Maven plugin configuration</li>
 *   <li>{@link org.apache.maven.api.model.Build} - Contains build configuration information</li>
 *   <li>{@link org.apache.maven.api.model.Profile} - Represents a build profile for conditional execution</li>
 * </ul>
 *
 * @since 4.0.0
 */
package org.apache.maven.api.model;
