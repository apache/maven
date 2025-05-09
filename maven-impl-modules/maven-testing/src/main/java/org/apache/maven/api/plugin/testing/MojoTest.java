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
package org.apache.maven.api.plugin.testing;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

import org.junit.jupiter.api.extension.ExtendWith;

/**
 * Annotation that enables Maven plugin (Mojo) testing support in JUnit tests.
 * When applied to a test class, it automatically sets up the testing environment
 * for Maven plugins, including dependency injection and parameter resolution.
 *
 * <p>This annotation works in conjunction with {@link InjectMojo} and {@link MojoParameter}
 * to provide a comprehensive testing framework for Maven plugins. It automatically registers
 * the {@link MojoExtension} which handles the plugin lifecycle and dependency injection.</p>
 *
 * <p>Example usage:</p>
 * <pre>
 * {@code
 * @MojoTest
 * class MyMojoTest {
 *     @Inject
 *     private SomeComponent component;
 *
 *     @Test
 *     @InjectMojo(goal = "my-goal")
 *     @MojoParameter(name = "parameter", value = "value")
 *     void testMojoExecution(MyMojo mojo) {
 *         // mojo is instantiated with the specified parameters
 *         // component is automatically injected
 *         mojo.execute();
 *         // verify execution results
 *     }
 *
 *     @Provides
 *     @Singleton
 *     SomeComponent provideMockedComponent() {
 *         return mock(SomeComponent.class);
 *     }
 * }
 * }
 * </pre>
 *
 * <p>The annotation supports:</p>
 * <ul>
 *   <li>Automatic Mojo instantiation and configuration</li>
 *   <li>Parameter injection via {@link MojoParameter}</li>
 *   <li>Component injection via {@link org.apache.maven.api.di.Inject}</li>
 *   <li>Mock component injection via {@link org.apache.maven.api.di.Provides}</li>
 *   <li>Custom POM configuration via {@link InjectMojo#pom()}</li>
 *   <li>Base directory configuration for test resources</li>
 * </ul>
 *
 * <p>This annotation replaces the legacy maven-plugin-testing-harness functionality
 * with a modern, annotation-based approach that integrates with JUnit Jupiter and
 * Maven's new dependency injection framework.</p>
 *
 * @see MojoExtension
 * @see InjectMojo
 * @see MojoParameter
 * @see org.apache.maven.api.di.Inject
 * @see org.apache.maven.api.di.Provides
 * @since 4.0.0
 */
@Retention(RetentionPolicy.RUNTIME)
@ExtendWith(MojoExtension.class)
@Target(ElementType.TYPE)
public @interface MojoTest {}
