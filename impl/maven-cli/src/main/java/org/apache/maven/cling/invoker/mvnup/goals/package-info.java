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
 * Maven Upgrade Tool Goals and Strategies.
 *
 * <p>This package contains the implementation of the Maven upgrade tool (mvnup) that helps
 * upgrade Maven projects to be compatible with Maven 4. The tool is organized around
 * a goal-based architecture with pluggable upgrade strategies.</p>
 *
 * <h2>Architecture Overview</h2>
 *
 * <h3>Goals</h3>
 * <ul>
 *   <li>{@link org.apache.maven.cling.invoker.mvnup.goals.Check} - Analyzes projects and reports needed upgrades</li>
 *   <li>{@link org.apache.maven.cling.invoker.mvnup.goals.Apply} - Applies upgrades to project files</li>
 *   <li>{@link org.apache.maven.cling.invoker.mvnup.goals.Help} - Displays usage information</li>
 * </ul>
 *
 * <h3>Upgrade Strategies</h3>
 * <p>The tool uses a strategy pattern to handle different types of upgrades:</p>
 * <ul>
 *   <li>{@link org.apache.maven.cling.invoker.mvnup.goals.ModelUpgradeStrategy} - Upgrades POM model versions (4.0.0 → 4.1.0)</li>
 *   <li>{@link org.apache.maven.cling.invoker.mvnup.goals.PluginUpgradeStrategy} - Upgrades plugin versions for Maven 4 compatibility</li>
 *   <li>{@link org.apache.maven.cling.invoker.mvnup.goals.InferenceStrategy} - Applies Maven 4.1.0+ inference optimizations</li>
 *   <li>{@link org.apache.maven.cling.invoker.mvnup.goals.CompatibilityFixStrategy} - Fixes Maven 4 compatibility issues</li>
 * </ul>
 *
 * <h3>Utility Classes</h3>
 * <ul>
 *   <li>{@link org.apache.maven.cling.invoker.mvnup.goals.StrategyOrchestrator} - Coordinates strategy execution</li>
 *   <li>{@link org.apache.maven.cling.invoker.mvnup.goals.PomDiscovery} - Discovers POM files in multi-module projects</li>
 *   <li>{@link org.apache.maven.cling.invoker.mvnup.goals.JDomUtils} - XML manipulation utilities</li>
 * </ul>
 *
 * <h2>Usage Examples</h2>
 *
 * <h3>Check for Available Upgrades</h3>
 * <pre>{@code
 * mvnup check
 * }</pre>
 *
 * <h3>Apply All Upgrades</h3>
 * <pre>{@code
 * mvnup apply --all
 * }</pre>
 *
 * <h3>Upgrade to Maven 4.1.0 with Inference</h3>
 * <pre>{@code
 * mvnup apply --model 4.1.0 --infer
 * }</pre>
 *
 * <h2>Extension Points</h2>
 *
 * <p>To add new upgrade strategies:</p>
 * <ol>
 *   <li>Implement {@link org.apache.maven.cling.invoker.mvnup.goals.UpgradeStrategy}</li>
 *   <li>Optionally extend {@link org.apache.maven.cling.invoker.mvnup.goals.AbstractUpgradeStrategy}</li>
 *   <li>Annotate with {@code @Named} and {@code @Singleton}</li>
 *   <li>Use {@code @Priority} to control execution order</li>
 * </ol>
 *
 * @since 4.0.0
 */
package org.apache.maven.cling.invoker.mvnup.goals;
