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

import java.io.IOException;
import java.util.Map;

import org.apache.maven.api.annotations.Consumer;
import org.apache.maven.api.annotations.Experimental;
import org.apache.maven.api.annotations.Nonnull;
import org.apache.maven.api.annotations.Nullable;
import org.apache.maven.api.di.Named;
import org.apache.maven.api.services.Source;
import org.apache.maven.api.settings.Settings;

/**
 * Parses settings in an additional syntax. Maven selects a parser for each settings source,
 * then performs interpolation, decryption, validation and merging on the returned settings.
 * If no parser supports a source, Maven uses its XML settings reader. Multiple parsers
 * supporting the same source are an error.
 * <p>
 * Parsers must be available in the container building the settings. In particular, a parser
 * supplied by a core extension cannot read the bootstrap settings needed to resolve that
 * extension. This SPI does not change settings file discovery or extension loading.
 *
 * @since 4.1.0
 */
@Experimental
@Consumer
@Named
public interface SettingsParser extends SpiService {
    /**
     * Boolean parsing option indicating whether unknown input should be rejected.
     */
    /**
     * Option that can be specified in the options map. The value should be a {@code Boolean};
     * when {@code true} or absent, unknown input is rejected.
     */
    String STRICT = "strict";

    /**
     * Determines whether this parser supports the source, without consuming its contents.
     * A source need not have a backing file; its location can also identify the syntax.
     *
     * @param source the settings source, never {@code null}
     * @return {@code true} if this parser supports the source
     */
    boolean supports(@Nonnull Source source);

    /**
     * Parses settings without interpolating, decrypting or merging them. The parser is
     * responsible for opening and closing any streams it obtains from the source.
     * Maven first requests strict parsing and, on a {@link SettingsParserException}, retries
     * with {@link #STRICT} set to {@code false}. A successful retry produces a warning for
     * the original problem. An I/O failure is not retried.
     *
     * @param source the settings source, never {@code null}
     * @param options parsing options, may be {@code null}; strict parsing is the default
     * @return the parsed settings, never {@code null}
     * @throws SettingsParserException if the contents cannot be parsed; diagnostics should
     *         identify the location without including credentials from the input
     * @throws IOException if the source cannot be read
     */
    @Nonnull
    Settings parse(@Nonnull Source source, @Nullable Map<String, ?> options)
            throws SettingsParserException, IOException;
}
