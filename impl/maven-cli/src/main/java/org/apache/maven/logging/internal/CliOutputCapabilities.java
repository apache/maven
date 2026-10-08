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
import java.util.Optional;

import org.apache.maven.api.services.OutputCapabilities;
import org.eclipse.sisu.Priority;

/**
 * Publishes immutable output descriptions for the lifetime of CLI logging configurations.
 * Sisu registration preserves the override's priority in both Plexus and Maven's DI bridge.
 */
@Named
@Singleton
@Priority(10)
public class CliOutputCapabilities implements OutputCapabilities {
    public static final OutputCapabilities EMPTY = snapshot(null, null, null);

    private volatile OutputCapabilities current = EMPTY;
    private Installation latest;

    @Override
    public Optional<Destination> getDestination() {
        return current.getDestination();
    }

    @Override
    public Optional<Charset> getEncoding() {
        return current.getEncoding();
    }

    @Override
    public Optional<Format> getFormat() {
        return current.getFormat();
    }

    /** Creates an immutable description; null properties mean unavailable information. */
    public static OutputCapabilities snapshot(Destination destination, Charset encoding, Format format) {
        return new Snapshot(
                Optional.ofNullable(destination), Optional.ofNullable(encoding), Optional.ofNullable(format));
    }

    /** Installs an immutable capture and returns an idempotent logging-lifecycle cleanup. */
    public synchronized AutoCloseable install(OutputCapabilities capabilities) {
        OutputCapabilities captured =
                new Snapshot(capabilities.getDestination(), capabilities.getEncoding(), capabilities.getFormat());
        Installation installed = new Installation(captured, latest);
        latest = installed;
        current = captured;
        return installed;
    }

    private final class Installation implements AutoCloseable {
        private final OutputCapabilities capabilities;
        private final Installation previous;
        private boolean closed;

        private Installation(OutputCapabilities capabilities, Installation previous) {
            this.capabilities = capabilities;
            this.previous = previous;
        }

        @Override
        public void close() {
            synchronized (CliOutputCapabilities.this) {
                closed = true;
                while (latest != null && latest.closed) {
                    latest = latest.previous;
                }
                current = latest == null ? EMPTY : latest.capabilities;
            }
        }
    }

    private record Snapshot(Optional<Destination> destination, Optional<Charset> encoding, Optional<Format> format)
            implements OutputCapabilities {
        @Override
        public Optional<Destination> getDestination() {
            return destination;
        }

        @Override
        public Optional<Charset> getEncoding() {
            return encoding;
        }

        @Override
        public Optional<Format> getFormat() {
            return format;
        }
    }
}
