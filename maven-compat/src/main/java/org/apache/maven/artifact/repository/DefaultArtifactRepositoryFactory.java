package org.apache.maven.artifact.repository;

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

import java.util.Arrays;
import java.util.List;

import org.apache.maven.artifact.UnknownRepositoryLayoutException;
import org.apache.maven.artifact.repository.layout.ArtifactRepositoryLayout;
import org.apache.maven.plugin.LegacySupport;
import org.apache.maven.repository.RepositorySystem;
import org.codehaus.plexus.component.annotations.Component;
import org.codehaus.plexus.component.annotations.Requirement;
import org.eclipse.aether.RepositorySystemSession;

/**
 * @author jdcasey
 */
@Component( role = ArtifactRepositoryFactory.class )
public class DefaultArtifactRepositoryFactory
    implements ArtifactRepositoryFactory
{

    @Requirement
    private org.apache.maven.repository.legacy.repository.ArtifactRepositoryFactory factory;

    @Requirement
    private LegacySupport legacySupport;

    @Requirement
    private RepositorySystem repositorySystem;

    public ArtifactRepositoryLayout getLayout( final String layoutId )
        throws UnknownRepositoryLayoutException
    {
        return factory.getLayout( layoutId );
    }

    public ArtifactRepository createDeploymentArtifactRepository( final String id, final String url, final String layoutId,
                                                                  final boolean uniqueVersion )
        throws UnknownRepositoryLayoutException
    {
        return injectSession( factory.createDeploymentArtifactRepository( id, url, layoutId, uniqueVersion ), false );
    }

    public ArtifactRepository createDeploymentArtifactRepository( final String id, final String url,
                                                                  final ArtifactRepositoryLayout repositoryLayout,
                                                                  final boolean uniqueVersion )
    {
        return injectSession( factory.createDeploymentArtifactRepository( id, url, repositoryLayout, uniqueVersion ),
                              false );
    }

    public ArtifactRepository createArtifactRepository( final String id, final String url, final String layoutId,
                                                        final ArtifactRepositoryPolicy snapshots,
                                                        final ArtifactRepositoryPolicy releases )
        throws UnknownRepositoryLayoutException
    {
        return injectSession( factory.createArtifactRepository( id, url, layoutId, snapshots, releases ), true );
    }

    public ArtifactRepository createArtifactRepository( final String id, final String url,
                                                        final ArtifactRepositoryLayout repositoryLayout,
                                                        final ArtifactRepositoryPolicy snapshots,
                                                        final ArtifactRepositoryPolicy releases )
    {
        return injectSession( factory.createArtifactRepository( id, url, repositoryLayout, snapshots, releases ),
                              true );

    }

    public void setGlobalUpdatePolicy( final String updatePolicy )
    {
        factory.setGlobalUpdatePolicy( updatePolicy );
    }

    public void setGlobalChecksumPolicy( final String checksumPolicy )
    {
        factory.setGlobalChecksumPolicy( checksumPolicy );
    }

    private ArtifactRepository injectSession( final ArtifactRepository repository, final boolean mirrors )
    {
        final RepositorySystemSession session = legacySupport.getRepositorySession();

        if ( session != null && repository != null && !isLocalRepository( repository ) )
        {
            final List<ArtifactRepository> repositories = Arrays.asList( repository );

            if ( mirrors )
            {
                repositorySystem.injectMirror( session, repositories );
            }

            repositorySystem.injectProxy( session, repositories );

            repositorySystem.injectAuthentication( session, repositories );
        }

        return repository;
    }

    private boolean isLocalRepository( final ArtifactRepository repository )
    {
        // unfortunately, the API doesn't allow to tell a remote repo and the local repo apart...
        return "local".equals( repository.getId() );
    }

}
