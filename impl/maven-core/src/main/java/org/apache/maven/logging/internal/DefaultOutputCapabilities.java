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
package org.apache.maven.logging.internal;

import javax.inject.Named;
import javax.inject.Singleton;

import java.nio.charset.Charset;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;

import org.apache.maven.logging.OutputCapabilities;

/**
 * Internal bridge between logging setup and the read-only component exposed to plugins.
 * Publication and restoration follow the logging lifecycle, not the build session.
 */
@Named
@Singleton
public class DefaultOutputCapabilities implements OutputCapabilities {
    public static final String REQUEST_DATA_KEY = "maven.logging.outputCapabilities";

    public static final OutputCapabilities UNKNOWN = snapshot(Destination.UNKNOWN, null);

    private volatile Snapshot current = (Snapshot) UNKNOWN;

    /** Internal access to an immutable description of the current logging configuration. */
    public Map<String, String> asMap() {
        return current.properties;
    }

    @Override
    public Destination getDestination() {
        return current.getDestination();
    }

    @Override
    public Optional<Charset> getEncoding() {
        return current.getEncoding();
    }

    /** Creates an immutable description for transport from logging setup to the container. */
    public static OutputCapabilities snapshot(Destination destination, Charset encoding) {
        return new Snapshot(Objects.requireNonNull(destination), encoding);
    }

    /**
     * Installs the current output description and returns its logging-lifecycle cleanup.
     * An obsolete cleanup must not overwrite a subsequently installed configuration.
     */
    public synchronized AutoCloseable install(OutputCapabilities capabilities) {
        Snapshot previous = current;
        Snapshot installed = new Snapshot(
                capabilities.getDestination(), capabilities.getEncoding().orElse(null));
        current = installed;
        return () -> {
            synchronized (DefaultOutputCapabilities.this) {
                if (current == installed) {
                    current = previous;
                }
            }
        };
    }

    private static final class Snapshot implements OutputCapabilities {
        private final Destination destination;
        private final Optional<Charset> encoding;
        private final Map<String, String> properties;

        private Snapshot(Destination destination, Charset encoding) {
            this.destination = destination;
            this.encoding = Optional.ofNullable(encoding);
            Map<String, String> values = new LinkedHashMap<>();
            values.put("destination", destination.name());
            if (encoding != null) {
                values.put("encoding", encoding.name());
            }
            this.properties = Collections.unmodifiableMap(values);
        }

        @Override
        public Destination getDestination() {
            return destination;
        }

        @Override
        public Optional<Charset> getEncoding() {
            return encoding;
        }
    }
}
