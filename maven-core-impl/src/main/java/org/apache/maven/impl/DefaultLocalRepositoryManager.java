package org.apache.maven.impl;

/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *  http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */

import java.nio.file.Path;
import java.nio.file.Paths;

import org.apache.maven.api.Artifact;
import org.apache.maven.api.LocalRepository;
import org.apache.maven.api.Metadata;
import org.apache.maven.api.RemoteRepository;
import org.apache.maven.api.Session;
import org.apache.maven.api.services.LocalRepositoryManager;
import org.eclipse.aether.repository.NoLocalRepositoryManagerException;

public class DefaultLocalRepositoryManager implements LocalRepositoryManager
{

    @Override
    public Path getPathForLocalArtifact( Session session, LocalRepository local, Artifact artifact )
    {
        DefaultSession s = (DefaultSession) session;
        return Paths.get( getManager( s, local ).getPathForLocalArtifact( s.toArtifact( artifact ) ) );
    }

    @Override
    public Path getPathForLocalMetadata( Session session, LocalRepository local, Metadata metadata )
    {
        DefaultSession s = (DefaultSession) session;
        return Paths.get( getManager( s, local ).getPathForLocalMetadata( s.toMetadata( metadata ) ) );
    }

    @Override
    public Path getPathForRemoteArtifact( Session session, LocalRepository local,
                                          RemoteRepository remote, Artifact artifact )
    {
        DefaultSession s = (DefaultSession) session;
        return Paths.get( getManager( s, local ).getPathForRemoteArtifact(
                s.toArtifact( artifact ), s.toRepository( remote ), null ) );
    }

    @Override
    public Path getPathForRemoteMetadata( Session session, LocalRepository local,
                                          RemoteRepository remote, Metadata metadata )
    {
        DefaultSession s = (DefaultSession) session;
        return Paths.get( getManager( s, local ).getPathForRemoteMetadata(
                s.toMetadata( metadata ), s.toRepository( remote ), null ) );
    }

    private org.eclipse.aether.repository.LocalRepositoryManager getManager(
            DefaultSession session, LocalRepository local )
    {
        try
        {
            return session.getLocalRepositoryProvider()
                    .newLocalRepositoryManager( session.getSession(), session.toRepository( local ) );
        }
        catch ( NoLocalRepositoryManagerException e )
        {
            throw new IllegalStateException( e );
        }
    }

}
