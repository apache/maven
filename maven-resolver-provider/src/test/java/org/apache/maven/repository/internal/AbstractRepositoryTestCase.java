package org.apache.maven.repository.internal;

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

import java.net.MalformedURLException;

import javax.inject.Inject;

import org.apache.maven.repository.internal.util.ConsoleRepositoryListener;
import org.apache.maven.repository.internal.util.ConsoleTransferListener;
import org.codehaus.plexus.PlexusContainer;
import org.codehaus.plexus.testing.PlexusTest;
import org.eclipse.aether.DefaultRepositorySystemSession;
import org.eclipse.aether.RepositorySystem;
import org.eclipse.aether.RepositorySystemSession;
import org.eclipse.aether.repository.LocalRepository;
import org.eclipse.aether.repository.RemoteRepository;
import org.junit.jupiter.api.BeforeEach;

import static org.codehaus.plexus.testing.PlexusExtension.getTestFile;

@PlexusTest
public abstract class AbstractRepositoryTestCase
{
    @Inject
    protected RepositorySystem system;

    @Inject
    protected PlexusContainer container;

    protected RepositorySystemSession session;

    @BeforeEach
    public void setUp()
        throws Exception
    {
        session = newMavenRepositorySystemSession( system );
    }

    protected PlexusContainer getContainer() {
        return container;
    }

    public static RepositorySystemSession newMavenRepositorySystemSession(RepositorySystem system )
    {
        DefaultRepositorySystemSession session = MavenRepositorySystemUtils.newSession();

        LocalRepository localRepo = new LocalRepository( "target/local-repo" );
        session.setLocalRepositoryManager( system.newLocalRepositoryManager( session, localRepo ) );

        session.setTransferListener( new ConsoleTransferListener() );
        session.setRepositoryListener( new ConsoleRepositoryListener() );

        return session;
    }

    public static RemoteRepository newTestRepository()
        throws MalformedURLException
    {
        return new RemoteRepository.Builder( "repo", "default",
                                             getTestFile( "target/test-classes/repo" ).toURI().toURL().toString() ).build();
    }
}
