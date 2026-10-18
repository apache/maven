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
package org.apache.maven.api;

import org.apache.maven.api.annotations.Experimental;
import org.apache.maven.api.annotations.Immutable;
import org.apache.maven.api.annotations.Nonnull;

import static org.apache.maven.api.ExtensibleEnums.dialect;

/**
 * Dialect.
 * <p>
 * This extensible enum has one defined value, {@link #XML},
 * but can be extended by registering a {@code org.apache.maven.api.spi.DialectProvider}.
 * <p>
 * Implementation must have {@code equals()} and {@code hashCode()} implemented, so implementations of this interface
 * can be used as keys.
 *
 * @since 4.1.0
 */
@Experimental
@Immutable
@SuppressWarnings("checkstyle:InterfaceIsType")
public interface Dialect extends ExtensibleEnum {

    /**
     * The "xml" dialect, the essence of Maven.
     */
    Dialect XML = dialect("xml");

    /**
     * Creates a new {@code Dialect} instance with the specified identifier.
     * <p>
     * The returned instance properly implements {@link Object#equals(Object)} and
     * {@link Object#hashCode()} based on the {@code id}, so it can safely be used
     * as a map key or in collections.
     * <p>
     * This factory method is intended for use by extension authors who implement
     * {@link org.apache.maven.api.spi.DialectProvider} or
     * {@link org.apache.maven.api.spi.ModelDialectProvider} and need to declare
     * a custom {@code Dialect} constant.
     *
     * @param id the unique identifier for the dialect, never {@code null}
     * @return a new {@code Dialect} instance with the given id
     * @throws NullPointerException if {@code id} is {@code null}
     * @since 4.1.0
     */
    @Nonnull
    static Dialect of(@Nonnull String id) {
        return dialect(id);
    }
}
