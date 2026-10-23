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
package org.apache.maven.model.interpolation.reflection;

import org.apache.maven.api.annotations.Nonnull;
import org.apache.maven.api.annotations.Nullable;

/**
 * @deprecated Use {@link org.apache.maven.utils.ReflectionValueExtractor} instead.
 */
@Deprecated(since = "4.0.0")
public class ReflectionValueExtractor {
    private ReflectionValueExtractor() {}

    public static Object evaluate(@Nonnull String expression, @Nullable Object root) throws IntrospectionException {
        return evaluate(expression, root, true);
    }

    public static Object evaluate(@Nonnull String expression, @Nullable Object root, boolean trimRootToken)
            throws IntrospectionException {
        try {
            return org.apache.maven.utils.ReflectionValueExtractor.evaluate(expression, root, trimRootToken, false);
        } catch (org.apache.maven.utils.IntrospectionException e) {
            IntrospectionException translated = e.getCause() != null
                    ? new IntrospectionException(e.getCause())
                    : new IntrospectionException(e.getMessage());
            // Retain the shared helper's failure location without changing the legacy cause contract.
            translated.setStackTrace(e.getStackTrace());
            throw translated;
        }
    }
}
