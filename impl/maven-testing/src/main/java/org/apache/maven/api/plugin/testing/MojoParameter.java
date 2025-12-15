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
import java.lang.annotation.Repeatable;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Specifies a parameter value for a Mojo in a Maven plugin test.
 * This annotation can be used to configure individual Mojo parameters
 * without requiring a full POM file.
 *
 * <p>The annotation is repeatable, allowing multiple parameters to be set
 * on a single test method or parameter. For multiple parameters, you can
 * either use multiple {@code @MojoParameter} annotations or a single
 * {@link MojoParameters} annotation.</p>
 *
 * <p>Example usage with a single parameter:</p>
 * <pre>
 * {@code
 * @Test
 * @InjectMojo(goal = "compile")
 * @MojoParameter(name = "source", value = "1.8")
 * void testCompilation(CompileMojo mojo) {
 *     mojo.execute();
 * }
 * }
 * </pre>
 *
 * <p>Example usage with multiple parameters:</p>
 * <pre>
 * {@code
 * @Test
 * @InjectMojo(goal = "compile")
 * @MojoParameter(name = "source", value = "1.8")
 * @MojoParameter(name = "target", value = "1.8")
 * @MojoParameter(name = "debug", value = "true")
 * void testCompilation(CompileMojo mojo) {
 *     mojo.execute();
 * }
 * }
 * </pre>
 *
 * @see MojoParameters
 * @see InjectMojo
 * @see MojoTest
 * @since 4.0.0
 */
@Target({ElementType.METHOD, ElementType.PARAMETER})
@Retention(RetentionPolicy.RUNTIME)
@Inherited
@Repeatable(MojoParameters.class)
public @interface MojoParameter {

    /**
     * The name of the Mojo parameter to set.
     * This should match the name of a parameter in the Mojo class,
     * as specified by its {@code @Parameter} annotation or field name.
     *
     * @return the parameter name
     */
    String name();

    /**
     * The value to set for the parameter.
     * The value can include Maven property expressions (e.g., "${project.version}").
     *
     * @return the parameter value
     */
    String value();

    /**
     * Whether to parse the value as XML.
     * When {@code true} (default), the value is parsed as XML content within the parameter element.
     * When {@code false}, the value is treated as plain text (useful for comma-separated lists).
     *
     * <p>Example with XML parsing enabled (default):</p>
     * <pre>
     * {@code
     * @MojoParameter(name = "items", value = "<item>one</item><item>two</item>")
     * }
     * </pre>
     *
     * <p>Example with XML parsing disabled:</p>
     * <pre>
     * {@code
     * @MojoParameter(name = "items", value = "one,two,three", xml = false)
     * }
     * </pre>
     *
     * @return {@code true} to parse as XML, {@code false} to treat as plain text
     * @since 4.0.0
     */
    boolean xml() default true;
}
