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
package org.apache.maven.api.services;

import java.nio.charset.Charset;
import java.util.Optional;

import org.apache.maven.api.Service;
import org.apache.maven.api.annotations.Experimental;
import org.apache.maven.api.annotations.Nonnull;

/**
 * Describes the current output of Maven logging, including plugin log messages.
 * Obtain this service through {@code session.getService(OutputCapabilities.class)}.
 * It does not describe files or streams written directly by a plugin.
 *
 * <p>Destination, encoding, and format are independent. An empty optional means that
 * the property cannot be determined, for example with a custom logging provider.
 * Machine-readable output can be written to a terminal, a file, or a redirected stream.
 *
 * <p>Plugins compiled against older Maven APIs can read an immutable
 * {@code Map<String, String>} from the execution request's data map under
 * {@code maven.logging.outputCapabilities}. The keys {@code destination} and
 * {@code format} contain the corresponding enum names when known; {@code encoding}
 * contains the canonical {@link Charset#name()}. Unknown properties are omitted.
 * A present empty map means no properties could be determined; an absent entry means
 * no capture is available. Clients should ignore unfamiliar keys and values.
 *
 * <p>The map is captured at the start of each Maven execution, before validation
 * and lifecycle callbacks. Reusing a request replaces its entry. Retained maps and
 * their collection views remain unchanged through cleanup, reconfiguration, and
 * subsequent invocations. In contrast, this service describes the current logging
 * configuration and can change between invocations in a resident process.
 *
 * @since 4.1.0
 */
@Experimental
public interface OutputCapabilities extends Service {
    /** The physical destination of Maven logging. */
    enum Destination {
        /** Output is attached to a terminal, independently of color or format. */
        CONSOLE,
        /** Maven or its logging provider opened an explicit file destination. */
        FILE,
        /** Output is not attached to a terminal; its final destination is not known. */
        REDIRECTED
    }

    /** The presentation format of Maven logging, independently of its destination. */
    enum Format {
        /** Text intended for people, including plain, rich, and verbose console modes. */
        HUMAN_READABLE,
        /** Structured output intended for tools, such as machine-mode JSON lines. */
        MACHINE_READABLE
    }

    /**
     * Returns the physical logging destination, if known.
     * @return the destination, or an empty optional when it cannot be determined
     */
    @Nonnull
    Optional<Destination> getDestination();

    /**
     * Returns the charset selected for the logging output route, if known.
     * Callers can use its encoder to check their exact characters, for example
     * the box-drawing characters used in a dependency tree. Encodability does not
     * guarantee font coverage, support for terminal controls, or that decoration
     * is appropriate for the output format.
     * @return the encoding, or an empty optional when it cannot be determined
     */
    @Nonnull
    Optional<Charset> getEncoding();

    /**
     * Returns the logging output format, if known.
     * @return the format, or an empty optional when it cannot be determined
     */
    @Nonnull
    Optional<Format> getFormat();
}
