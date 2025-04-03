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
 * Annotation used in Maven plugin tests to inject and configure a Mojo instance.
 * This annotation can be applied to either test methods or parameters to specify
 * which Mojo should be instantiated and how it should be configured.
 *
 * <p>The annotation requires a {@code goal} attribute to specify which Mojo goal
 * should be instantiated. Optionally, a custom {@code pom} file can be specified
 * to provide specific configuration for the test.</p>
 *
 * <p>Example usage on a test method:</p>
 * <pre>
 * {@code
 * @Test
 * @InjectMojo(goal = "compile")
 * void testCompileMojo(CompileMojo mojo) {
 *     mojo.execute();
 *     // verify compilation results
 * }
 * }
 * </pre>
 *
 * <p>Example usage with a custom POM:</p>
 * <pre>
 * {@code
 * @Test
 * @InjectMojo(
 *     goal = "compile",
 *     pom = "src/test/resources/test-pom.xml"
 * )
 * void testCompileMojoWithCustomConfig(CompileMojo mojo) {
 *     mojo.execute();
 *     // verify compilation results
 * }
 * }
 * </pre>
 *
 * <p>The annotation can be used in conjunction with {@link MojoParameter} to provide
 * specific parameter values for the Mojo:</p>
 * <pre>
 * {@code
 * @Test
 * @InjectMojo(goal = "compile")
 * @MojoParameter(name = "source", value = "1.8")
 * @MojoParameter(name = "target", value = "1.8")
 * void testCompileMojoWithParameters(CompileMojo mojo) {
 *     mojo.execute();
 *     // verify compilation results
 * }
 * }
 * </pre>
 *
 * @see MojoTest
 * @see MojoParameter
 * @see MojoExtension
 * @since 4.0.0
 */
@Target({ElementType.METHOD, ElementType.PARAMETER})
@Retention(RetentionPolicy.RUNTIME)
@Inherited
public @interface InjectMojo {

    /**
     * Specifies the goal of the Mojo to instantiate.
     * This is a required attribute that maps to the Mojo's {@code @Mojo(name = "...")}
     * annotation value.
     *
     * @return the goal name of the Mojo to test
     */
    String goal();

    /**
     * Specifies an optional POM file to use for Mojo configuration.
     * The path is relative to the test class location.
     *
     * <p>If not specified, the default project configuration will be used.</p>
     *
     * @return the path to a custom POM file, or an empty string to use defaults
     */
    String pom() default "";
}
