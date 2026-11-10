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
package org.apache.maven.plugin.internal;

import java.io.File;

/**
 * Evaluates mojo parameter expressions and aligns paths to the project base directory.
 * Used by {@link V4MojoConfigurator} to resolve plugin parameter values.
 */
public interface MojoExpressionEvaluator {

    /**
     * Evaluates the given expression, optionally coercing the result to the given type.
     *
     * <p>This method declares {@code throws Exception} rather than
     * {@code throws MojoExpressionEvaluationException} intentionally: tying the public SPI
     * signature to a specific exception hierarchy would force implementors to depend on
     * {@code maven-core} internals. Callers should catch {@code Exception} and wrap as needed.
     *
     * @param expr the expression to evaluate (e.g. {@code ${project.version}})
     * @param type the desired return type, or {@code null} for no coercion
     * @return the evaluated value, or {@code null}
     * @throws Exception if evaluation fails
     */
    Object evaluate(String expr, Class<?> type) throws Exception;

    /**
     * Aligns a relative {@link File} to the project base directory.
     *
     * @param file the file to align
     * @return the aligned file
     */
    File alignToBaseDirectory(File file);
}
