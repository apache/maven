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
package org.apache.maven.logging;

import java.nio.charset.Charset;
import java.util.Optional;

/**
 * Describes the current destination of Maven logging, including plugin log messages.
 * The information follows logging configuration and is shared by all projects in a build.
 * It does not describe files or streams written directly by a plugin.
 *
 * <p>Plugins compiled against older Maven APIs can obtain the same information without
 * referencing this interface through
 * {@code session.getRequest().getData().get("maven.logging.outputCapabilities")}.
 * The value is an immutable {@code Map<String, String>}. Its {@code destination}
 * entry is always present and contains {@code CONSOLE}, {@code FILE}, {@code REDIRECTED},
 * or {@code UNKNOWN}. Its {@code encoding} entry contains the canonical
 * {@link Charset#name()} when known and is otherwise absent, independently of destination.
 * An absent request-data entry means the service is unavailable (for example, on older
 * Maven versions); a present map with {@code UNKNOWN} means detection is unavailable.
 * Clients should ignore additional keys and treat unfamiliar destinations as unknown.
 *
 * <p>The map captures the logging configuration when
 * {@link org.apache.maven.Maven#execute(org.apache.maven.execution.MavenExecutionRequest)}
 * begins, before validation and lifecycle callbacks. The entry is absent before execution
 * unless it remains from an earlier execution of the same request. Each execution replaces
 * the entry with a fresh capture, including when the request is copied or reused.
 * The map and its collection views remain unchanged through logging cleanup,
 * reconfiguration, and subsequent invocations. A retained map captured while information
 * is unknown remains unknown. Cleanup does not remove the entry from the request.
 * Unlike the map, this component's getters describe the current logging configuration
 * and may therefore differ from a retained request's map after reconfiguration or cleanup.
 */
public interface OutputCapabilities {
    /** The known kind of logging destination. */
    enum Destination {
        /** Output is attached to a terminal, independently of whether color is enabled. */
        CONSOLE,
        /** Maven or its logging provider opened an explicit file destination. */
        FILE,
        /** Output is not attached to a terminal; its final destination is not known. */
        REDIRECTED,
        /** The logging destination could not be determined. */
        UNKNOWN
    }

    /**
     * Returns the known kind of logging destination.
     * @return the destination, never {@code null}
     */
    Destination getDestination();

    /**
     * Returns the charset selected for the logging output route, if known.
     * Encoding may be known even when the destination is unknown. It does not imply
     * support for terminal control sequences or guarantee that a font contains a glyph.
     * @return the output charset, or an empty optional when it cannot be determined
     */
    Optional<Charset> getEncoding();
}
