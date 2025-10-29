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
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Indicates that a {@link Provides} method contributes to an aggregated collection
 * rather than replacing it.
 *
 * <p>Maven DI automatically aggregates beans into collections when injecting {@code List<T>}
 * or {@code Map<String, T>}. By default, if an explicit {@code @Provides} method returns
 * a collection of the same type, it <strong>replaces</strong> the auto-aggregation.
 * The {@code @Aggregate} annotation changes this behavior to <strong>contribute</strong>
 * entries to the aggregated collection instead.</p>
 *
 * <h2>Collection Aggregation Rules</h2>
 *
 * <h3>Without explicit {@code @Provides}:</h3>
 * <ul>
 *   <li>{@code List<T>} - automatically aggregates all beans of type {@code T}</li>
 *   <li>{@code Map<String, T>} - automatically aggregates all {@code @Named} beans of type {@code T},
 *       using their name as the key</li>
 * </ul>
 *
 * <h3>With {@code @Provides} (no {@code @Aggregate}):</h3>
 * <ul>
 *   <li>The explicit provider <strong>replaces</strong> auto-aggregation</li>
 *   <li>Only the provided collection is available for injection</li>
 * </ul>
 *
 * <h3>With {@code @Provides @Aggregate}:</h3>
 * <ul>
 *   <li>The provider <strong>contributes to</strong> auto-aggregation</li>
 *   <li>Multiple {@code @Aggregate} providers can coexist and all contribute</li>
 *   <li>If both {@code @Aggregate} and non-{@code @Aggregate} providers exist,
 *       the non-{@code @Aggregate} provider takes precedence</li>
 * </ul>
 *
 * <h2>Usage Examples</h2>
 *
 * <h3>Contributing to a Map</h3>
 * <pre>{@code
 * @Provides
 * @Aggregate
 * Map<String, PluginService> corePlugins() {
 *     Map<String, PluginService> plugins = new HashMap<>();
 *     plugins.put("compile", new CompilePlugin());
 *     plugins.put("test", new TestPlugin());
 *     return plugins;
 * }
 *
 * @Provides
 * @Aggregate
 * Map<String, PluginService> extraPlugins() {
 *     Map<String, PluginService> plugins = new HashMap<>();
 *     plugins.put("deploy", new DeployPlugin());
 *     return plugins;
 * }
 *
 * // Injection point receives all entries
 * @Inject
 * Map<String, PluginService> allPlugins; // Contains: compile, test, deploy
 * }</pre>
 *
 * <h3>Contributing to a List</h3>
 * <pre>{@code
 * @Provides
 * @Aggregate
 * List<Validator> customValidators() {
 *     return Arrays.asList(
 *         new PomValidator(),
 *         new DependencyValidator()
 *     );
 * }
 *
 * @Inject
 * List<Validator> allValidators; // Contains all @Named Validator beans + custom ones
 * }</pre>
 *
 * <h3>Contributing Single Beans</h3>
 * <pre>{@code
 * // Single bean contributions are implicitly aggregated (no @Aggregate needed)
 * @Provides
 * @Named("foo")
 * MyService foo() {
 *     return new FooService();
 * }
 *
 * @Provides
 * @Named("bar")
 * MyService bar() {
 *     return new BarService();
 * }
 *
 * @Inject
 * Map<String, MyService> services; // Contains: foo, bar
 * }</pre>
 *
 * <h3>Replacing Auto-Aggregation</h3>
 * <pre>{@code
 * @Named("service1")
 * class Service1 implements MyService {}
 *
 * @Named("service2")
 * class Service2 implements MyService {}
 *
 * // Without @Aggregate, this REPLACES the auto-aggregated map
 * @Provides
 * Map<String, MyService> customMap() {
 *     return Map.of("only", new OnlyService());
 * }
 *
 * @Inject
 * Map<String, MyService> services; // Contains only: "only" -> OnlyService
 * }</pre>
 *
 * <h2>Priority and Ordering</h2>
 * <p>When contributing to {@code List<T>}, entries follow priority ordering rules:</p>
 * <ul>
 *   <li>Beans with {@link Priority} annotation are ordered by priority (highest first)</li>
 *   <li>Beans without priority come after prioritized beans</li>
 *   <li>Collections contributed via {@code @Aggregate} are merged respecting these rules</li>
 * </ul>
 *
 * <h2>Duplicate Keys</h2>
 * <p>When multiple {@code @Aggregate} providers contribute the same key to a {@code Map<String, T>},
 * the behavior is last-write-wins (though this may result in a warning or error in future versions).</p>
 *
 * <h2>Plugin Architecture Pattern</h2>
 * <p>This annotation is particularly useful for Maven's plugin architecture, where different
 * modules need to register their services without depending on a central registry:</p>
 * <pre>{@code
 * // In plugin module A
 * @Provides
 * @Aggregate
 * Map<String, LifecycleProvider> lifecycleProviders() {
 *     return Map.of("clean", new CleanLifecycle());
 * }
 *
 * // In plugin module B
 * @Provides
 * @Aggregate
 * Map<String, LifecycleProvider> moreLifecycleProviders() {
 *     return Map.of("deploy", new DeployLifecycle());
 * }
 *
 * // In core
 * @Inject
 * Map<String, LifecycleProvider> allLifecycles; // Contains contributions from all modules
 * }</pre>
 *
 * @since 4.0.0
 * @see Provides
 * @see Named
 * @see Inject
 */
@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface Aggregate {}
