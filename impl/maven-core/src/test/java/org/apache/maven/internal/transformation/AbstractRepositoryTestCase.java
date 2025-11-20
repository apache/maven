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
package org.apache.maven.internal.transformation;

import javax.inject.Inject;

import java.net.MalformedURLException;
import java.util.List;

import org.apache.maven.SimpleLookup;
import org.apache.maven.execution.DefaultMavenExecutionRequest;
import org.apache.maven.execution.DefaultMavenExecutionResult;
import org.apache.maven.execution.MavenSession;
import org.apache.maven.impl.DefaultRepositoryFactory;
import org.apache.maven.impl.InternalSession;
import org.apache.maven.impl.model.DefaultInterpolator;
import org.apache.maven.impl.resolver.scopes.Maven4ScopeManagerConfiguration;
import org.apache.maven.internal.impl.DefaultSession;
import org.codehaus.plexus.PlexusContainer;
import org.codehaus.plexus.testing.PlexusTest;
import org.eclipse.aether.DefaultRepositorySystemSession;
import org.eclipse.aether.RepositoryListener;
import org.eclipse.aether.RepositorySystem;
import org.eclipse.aether.RepositorySystemSession;
import org.eclipse.aether.internal.impl.DefaultChecksumPolicyProvider;
import org.eclipse.aether.internal.impl.DefaultRemoteRepositoryManager;
import org.eclipse.aether.internal.impl.DefaultRepositoryKeyFunctionFactory;
import org.eclipse.aether.internal.impl.DefaultUpdatePolicyAnalyzer;
import org.eclipse.aether.internal.impl.scope.ScopeManagerImpl;
import org.eclipse.aether.repository.LocalRepository;
import org.eclipse.aether.repository.RemoteRepository;
import org.eclipse.aether.transfer.TransferListener;
import org.junit.jupiter.api.BeforeEach;
import org.mockito.Mockito;

import static org.codehaus.plexus.testing.PlexusExtension.getTestFile;

@PlexusTest
public abstract class AbstractRepositoryTestCase {
    @Inject
    protected RepositorySystem system;

    @Inject
    protected PlexusContainer container;

    protected RepositorySystemSession session;

    @BeforeEach
    public void setUp() throws Exception {
        session = newMavenRepositorySystemSession(system);
    }

    protected PlexusContainer getContainer() {
        return container;
    }

    public RepositorySystemSession newMavenRepositorySystemSession(RepositorySystem system) {
        DefaultRepositorySystemSession rsession = new DefaultRepositorySystemSession(h -> false);
        rsession.setScopeManager(new ScopeManagerImpl(Maven4ScopeManagerConfiguration.INSTANCE));

        LocalRepository localRepo = new LocalRepository("target/local-repo");
        rsession.setLocalRepositoryManager(system.newLocalRepositoryManager(rsession, localRepo));

        rsession.setTransferListener(Mockito.mock(TransferListener.class));
        rsession.setRepositoryListener(Mockito.mock(RepositoryListener.class));

        DefaultMavenExecutionRequest request = new DefaultMavenExecutionRequest();
        MavenSession mavenSession = new MavenSession(rsession, request, new DefaultMavenExecutionResult());
        DefaultSession session =
                new DefaultSession(mavenSession, null, null, null, new SimpleLookup(getSessionServices()), null);
        InternalSession.associate(rsession, session);

        return rsession;
    }

    protected List<Object> getSessionServices() {
        return List.of(
                new DefaultRepositoryFactory(new DefaultRemoteRepositoryManager(
                        new DefaultUpdatePolicyAnalyzer(),
                        new DefaultChecksumPolicyProvider(),
                        new DefaultRepositoryKeyFunctionFactory())),
                new DefaultInterpolator());
    }

    public static RemoteRepository newTestRepository() throws MalformedURLException {
        return new RemoteRepository.Builder(
                        "repo",
                        "default",
                        getTestFile("target/test-classes/repo").toURI().toURL().toString())
                .build();
    }
}
