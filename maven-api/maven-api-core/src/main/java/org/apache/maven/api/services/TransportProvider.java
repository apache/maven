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

import org.apache.maven.api.RemoteRepository;
import org.apache.maven.api.Service;
import org.apache.maven.api.Session;
import org.apache.maven.api.annotations.Consumer;
import org.apache.maven.api.annotations.Experimental;
import org.apache.maven.api.annotations.Nonnull;

/**
 * Transporter provider is a service that provides somewhat trivial transport capabilities backed by Maven internals.
 * This API does not try to cover all the requirements out there, just the basic ones, and is intentionally simple.
 * If plugin or extension needs anything more complex feature wise (i.e. HTTP range support or alike) it should
 * probably roll its own.
 * <p>
 * This implementation is backed by Maven Resolver API, supported protocols and transport selection depends on it. If
 * resolver preference regarding transport is altered, it will affect this service as well.
 *
 * @since 4.0.0
 */
@Experimental
@Consumer
public interface TransportProvider extends Service {
    /**
     * Provides new {@link Transport} instance for given {@link RemoteRepository}, if possible.
     *
     * @throws TransportProviderException if passed in remote repository has invalid remote URL or unsupported protocol.
     */
    @Nonnull
    Transport transport(@Nonnull Session session, @Nonnull RemoteRepository repository);
}
