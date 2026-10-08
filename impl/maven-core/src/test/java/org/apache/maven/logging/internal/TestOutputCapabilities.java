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

import java.nio.charset.Charset;
import java.util.Map;
import java.util.Optional;

import org.apache.maven.api.services.OutputCapabilities;
import org.apache.maven.impl.DefaultOutputCapabilities;

public class TestOutputCapabilities implements OutputCapabilities {
    private volatile OutputCapabilities current = new DefaultOutputCapabilities();

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

    public Map<String, String> asMap() {
        return OutputCapabilitiesData.capture(this);
    }

    public AutoCloseable install(OutputCapabilities capabilities) {
        OutputCapabilities previous = current;
        current = capabilities;
        return () -> current = previous;
    }

    public static OutputCapabilities snapshot(Destination destination, Charset encoding, Format format) {
        return new OutputCapabilities() {
            public Optional<Destination> getDestination() {
                return Optional.ofNullable(destination);
            }

            public Optional<Charset> getEncoding() {
                return Optional.ofNullable(encoding);
            }

            public Optional<Format> getFormat() {
                return Optional.ofNullable(format);
            }
        };
    }
}
