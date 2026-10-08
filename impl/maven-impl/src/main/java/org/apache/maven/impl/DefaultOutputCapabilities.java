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
package org.apache.maven.impl;

import java.nio.charset.Charset;
import java.util.Optional;

import org.apache.maven.api.di.Named;
import org.apache.maven.api.di.Priority;
import org.apache.maven.api.di.Singleton;
import org.apache.maven.api.services.OutputCapabilities;

/** Fallback for environments that do not supply information about their logging output. */
@Named
@Singleton
@Priority(-1)
public class DefaultOutputCapabilities implements OutputCapabilities {
    @Override
    public Optional<Destination> getDestination() {
        return Optional.empty();
    }

    @Override
    public Optional<Charset> getEncoding() {
        return Optional.empty();
    }

    @Override
    public Optional<Format> getFormat() {
        return Optional.empty();
    }
}
