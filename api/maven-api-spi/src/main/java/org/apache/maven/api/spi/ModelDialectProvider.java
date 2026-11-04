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

import org.apache.maven.api.Dialect;
import org.apache.maven.api.annotations.Consumer;
import org.apache.maven.api.annotations.Experimental;
import org.apache.maven.api.annotations.Nonnull;
import org.apache.maven.api.di.Named;

/**
 * SPI to provide a symmetric pair of {@link ModelParser} and {@link ModelWriter} for a given {@link Dialect}.
 * <p>
 * Implement this interface to register support for reading and writing Maven models in an alternative dialect.
 * The implementation must be registered as a named DI component so that Maven can discover it.
 *
 * @since 4.1.0
 * @see ModelParser
 * @see ModelWriter
 * @see DialectProvider
 */
@Experimental
@Consumer
@Named
public interface ModelDialectProvider extends SpiService {

    /**
     * The {@link Dialect} this provider handles.
     *
     * @return the dialect, never {@code null}
     */
    @Nonnull
    Dialect getDialect();

    /**
     * The dialect-specific parser used to read models.
     *
     * @return the model parser, never {@code null}
     */
    @Nonnull
    ModelParser getModelParser();

    /**
     * The dialect-specific writer used to write models.
     *
     * @return the model writer, never {@code null}
     */
    @Nonnull
    ModelWriter getModelWriter();
}
