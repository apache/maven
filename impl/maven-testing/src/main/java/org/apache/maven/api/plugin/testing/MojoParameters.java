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
 * Container annotation for multiple {@link MojoParameter} annotations.
 * This annotation is automatically used by Java when multiple {@code @MojoParameter}
 * annotations are applied to the same element.
 *
 * <p>While this annotation can be used directly, it's generally more convenient
 * to use multiple {@code @MojoParameter} annotations, which Java will automatically
 * wrap in this container annotation.</p>
 *
 * <p>Example of direct usage:</p>
 * <pre>
 * {@code
 * @Test
 * @InjectMojo(goal = "compile")
 * @MojoParameters({
 *     @MojoParameter(name = "source", value = "1.8"),
 *     @MojoParameter(name = "target", value = "1.8"),
 *     @MojoParameter(name = "debug", value = "true")
 * })
 * void testCompilation(CompileMojo mojo) {
 *     mojo.execute();
 * }
 * }
 * </pre>
 *
 * <p>Equivalent usage with repeatable annotation:</p>
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
 * @see MojoParameter
 * @see InjectMojo
 * @see MojoTest
 * @since 4.0.0
 */
@Target({ElementType.METHOD, ElementType.PARAMETER})
@Retention(RetentionPolicy.RUNTIME)
@Inherited
public @interface MojoParameters {

    /**
     * The array of {@link MojoParameter} annotations.
     *
     * @return the array of parameter annotations
     */
    MojoParameter[] value();
}
