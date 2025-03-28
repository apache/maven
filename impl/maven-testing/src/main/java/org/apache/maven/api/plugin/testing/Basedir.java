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
import java.lang.annotation.Inherited;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Specifies the base directory for test resources in Maven plugin tests.
 * This annotation can be applied to test classes or methods to define where test resources
 * (such as POM files, source files, and other test artifacts) are located.
 *
 * <p>If not specified, the plugin's base directory will be used as the default.</p>
 *
 * <p>Example usage on class level:</p>
 * <pre>
 * {@code
 * @MojoTest
 * @Basedir("src/test/resources/my-test-project")
 * class MyMojoTest {
 *     @Test
 *     @InjectMojo(goal = "compile")
 *     void testCompilation(MyMojo mojo) {
 *         // Test resources will be loaded from src/test/resources/my-test-project
 *         mojo.execute();
 *     }
 * }
 * }
 * </pre>
 *
 * <p>Example usage on method level:</p>
 * <pre>
 * {@code
 * @MojoTest
 * class MyMojoTest {
 *     @Test
 *     @Basedir("src/test/resources/specific-test-case")
 *     @InjectMojo(goal = "compile")
 *     void testSpecificCase(MyMojo mojo) {
 *         // Test resources will be loaded from src/test/resources/specific-test-case
 *         mojo.execute();
 *     }
 * }
 * }
 * </pre>
 *
 * <p>When applied at both class and method level, the method-level annotation takes precedence.</p>
 *
 * @see MojoTest
 * @see MojoExtension
 * @since 4.0.0
 */
@Target({ElementType.TYPE, ElementType.METHOD})
@Retention(RetentionPolicy.RUNTIME)
@Inherited
public @interface Basedir {

    /**
     * The path to the base directory for test resources.
     * The path can be absolute or relative to the test class location.
     *
     * @return the base directory path, or an empty string to use the default
     */
    String value() default "";
}
