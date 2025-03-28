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
package org.apache.maven.api.di.testing;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

import org.junit.jupiter.api.extension.ExtendWith;

/**
 * Annotation that enables Maven's dependency injection support in JUnit tests.
 * When applied to a test class, it automatically sets up the DI container and
 * performs injection into test instances.
 *
 * <p>This annotation is a convenient way to use {@link MavenDIExtension} without
 * explicitly using {@code @ExtendWith}. It provides the same functionality as the
 * legacy Plexus test support but uses Maven's new DI framework.</p>
 *
 * <p>Example usage:</p>
 * <pre>
 * {@code
 * @MavenDITest
 * class MyComponentTest {
 *     @Inject
 *     private MyComponent component;
 *
 *     @Test
 *     void testComponentBehavior() {
 *         // component is automatically injected
 *         assertNotNull(component);
 *         // perform test
 *     }
 * }
 * }
 * </pre>
 *
 * <p>The annotation supports:</p>
 * <ul>
 *   <li>Constructor injection</li>
 *   <li>Field injection</li>
 *   <li>Method injection</li>
 *   <li>Automatic component discovery</li>
 *   <li>Lifecycle management of injected components</li>
 * </ul>
 *
 * @see MavenDIExtension
 * @see org.apache.maven.api.di.Inject
 * @since 4.0.0
 */
@Retention(RetentionPolicy.RUNTIME)
@ExtendWith(MavenDIExtension.class)
@Target(ElementType.TYPE)
public @interface MavenDITest {}
