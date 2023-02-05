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
package org.apache.maven.internal.impl;

import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Singleton;

import java.net.URI;
import java.net.URISyntaxException;

import org.apache.maven.api.RemoteRepository;
import org.apache.maven.api.Session;
import org.apache.maven.api.services.Transport;
import org.apache.maven.api.services.TransportProvider;
import org.apache.maven.api.services.TransportProviderException;
import org.eclipse.aether.spi.connector.transport.TransporterProvider;
import org.eclipse.aether.transfer.NoTransporterException;

import static java.util.Objects.requireNonNull;

@Named
@Singleton
public class DefaultTransportProvider implements TransportProvider {
    private final org.eclipse.aether.spi.connector.transport.TransporterProvider transporterProvider;

    @Inject
    public DefaultTransportProvider(TransporterProvider transporterProvider) {
        this.transporterProvider = requireNonNull(transporterProvider);
    }

    @Override
    public Transport transport(Session session, RemoteRepository repository) {
        try {
            URI baseURI = new URI(repository.getUrl());
            return new DefaultTransport(
                    baseURI,
                    transporterProvider.newTransporter(
                            ((DefaultSession) session).getSession(),
                            ((DefaultRemoteRepository) repository).getRepository()));
        } catch (URISyntaxException e) {
            throw new TransportProviderException("Remote repository URL invalid", e);
        } catch (NoTransporterException e) {
            throw new TransportProviderException("Unsupported remote repository", e);
        }
    }
}
