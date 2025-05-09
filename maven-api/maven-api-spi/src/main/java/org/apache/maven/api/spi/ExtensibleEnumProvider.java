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
package org.apache.maven.api.spi;

import java.util.Collection;

import org.apache.maven.api.ExtensibleEnum;
import org.apache.maven.api.annotations.Consumer;
import org.apache.maven.api.annotations.Experimental;
import org.apache.maven.api.annotations.Nonnull;

/**
 * An SPI interface to extend Maven with new enum values for extensible enumerations.
 * <p>
 * Maven uses extensible enumerations to allow plugins and extensions to add new values
 * to various categories like languages, scopes, and packaging types. This interface is the
 * base for all providers that register such extensions.
 * <p>
 * Implementations of this interface are discovered through the Java ServiceLoader mechanism.
 * Each implementation must be registered in a {@code META-INF/services/} file corresponding
 * to the specific provider interface being implemented.
 * <p>
 * Example implementation for a custom language provider:
 * <pre>
 * public class CustomLanguageProvider implements LanguageProvider {
 *     public Collection&lt;Language&gt; provides() {
 *         return Arrays.asList(
 *             language("kotlin"),
 *             language("scala")
 *         );
 *     }
 * }
 * </pre>
 *
 * @param <T> The type of extensible enum to extend
 * @since 4.0.0
 */
@Experimental
@Consumer
public interface ExtensibleEnumProvider<T extends ExtensibleEnum> extends SpiService {

    /**
     * Provides new values for the extensible enum.
     * <p>
     * This method is called by Maven during initialization to collect all custom enum values
     * that should be registered. The returned collection should contain all the enum values
     * that this provider wants to contribute.
     * <p>
     * The values returned by this method should be created using the appropriate factory methods
     * for the specific enum type, such as {@code language()}, {@code projectScope()}, or
     * {@code pathScope()}.
     *
     * @return a non-null collection of enum instances to register
     */
    @Nonnull
    Collection<T> provides();
}
