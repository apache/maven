package org.apache.maven.plugin.testing.stubs;

/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */

import java.io.IOException;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.apache.maven.artifact.Artifact;
import org.apache.maven.artifact.metadata.ArtifactMetadataSource;
import org.apache.maven.artifact.repository.ArtifactRepository;
import org.apache.maven.artifact.resolver.ArtifactNotFoundException;
import org.apache.maven.artifact.resolver.ArtifactResolutionException;
import org.apache.maven.artifact.resolver.ArtifactResolutionRequest;
import org.apache.maven.artifact.resolver.ArtifactResolutionResult;
import org.apache.maven.artifact.resolver.ArtifactResolver;
import org.apache.maven.artifact.resolver.ResolutionListener;
import org.apache.maven.artifact.resolver.filter.ArtifactFilter;
import org.apache.maven.plugin.testing.ArtifactStubFactory;
import org.apache.maven.wagon.events.TransferListener;

/**
 * Stub resolver. The constructor allows the specification of the exception to throw so that handling can be tested too.
 *
 * @author <a href="mailto:brianf@apache.org">Brian Fox</a>
 * @version $Id$
 */
public class StubArtifactResolver
    implements ArtifactResolver
{
    private boolean throwArtifactResolutionException;

    private boolean throwArtifactNotFoundException;

    private ArtifactStubFactory factory;

    /**
     * Default constructor
     *
     * @param factory
     * @param throwArtifactResolutionException
     * @param throwArtifactNotFoundException
     */
    public StubArtifactResolver( ArtifactStubFactory factory, boolean throwArtifactResolutionException,
                                boolean throwArtifactNotFoundException )
    {
        this.throwArtifactNotFoundException = throwArtifactNotFoundException;
        this.throwArtifactResolutionException = throwArtifactResolutionException;
        this.factory = factory;
    }

    /**
     * Creates dummy file and sets it in the artifact to simulate resolution
     *
     * @see org.apache.maven.artifact.resolver.ArtifactResolver#resolve(org.apache.maven.artifact.Artifact, java.util.List, org.apache.maven.artifact.repository.ArtifactRepository)
     */
    public void resolve( Artifact artifact, List<ArtifactRepository> remoteRepositories,
                         ArtifactRepository localRepository )
        throws ArtifactResolutionException, ArtifactNotFoundException
    {
        if ( !this.throwArtifactNotFoundException && !this.throwArtifactResolutionException )
        {
            try
            {
                if ( factory != null )
                {
                    factory.setArtifactFile( artifact, factory.getWorkingDir() );
                }
            }
            catch ( IOException e )
            {
                throw new ArtifactResolutionException( "IOException: " + e.getMessage(), artifact, e );
            }
        }
        else
        {
            if ( throwArtifactResolutionException )
            {
                throw new ArtifactResolutionException( "Catch!", artifact );
            }

            throw new ArtifactNotFoundException( "Catch!", artifact );
        }
    }

    /**
     * @return <code>null</code>.
     * @see org.apache.maven.artifact.resolver.ArtifactResolver#resolveTransitively(java.util.Set, org.apache.maven.artifact.Artifact, java.util.List, org.apache.maven.artifact.repository.ArtifactRepository, org.apache.maven.artifact.metadata.ArtifactMetadataSource)
     */
    public ArtifactResolutionResult resolveTransitively( Set<Artifact> artifacts, Artifact originatingArtifact,
                                                         List<ArtifactRepository> remoteRepositories,
                                                         ArtifactRepository localRepository,
                                                         ArtifactMetadataSource source )
        throws ArtifactResolutionException, ArtifactNotFoundException
    {
        return null;
    }

    /**
     * @return <code>null</code>.
     * @see org.apache.maven.artifact.resolver.ArtifactResolver#resolveTransitively(java.util.Set, org.apache.maven.artifact.Artifact, java.util.List, org.apache.maven.artifact.repository.ArtifactRepository, org.apache.maven.artifact.metadata.ArtifactMetadataSource, java.util.List)
     */
    public ArtifactResolutionResult resolveTransitively( Set<Artifact> artifacts, Artifact originatingArtifact,
                                                         List<ArtifactRepository> remoteRepositories,
                                                         ArtifactRepository localRepository,
                                                         ArtifactMetadataSource source,
                                                         List<ResolutionListener> listeners )
        throws ArtifactResolutionException, ArtifactNotFoundException
    {
        return null;
    }

    /**
     * @return <code>null</code>.
     * @see org.apache.maven.artifact.resolver.ArtifactResolver#resolveTransitively(java.util.Set, org.apache.maven.artifact.Artifact, org.apache.maven.artifact.repository.ArtifactRepository, java.util.List, org.apache.maven.artifact.metadata.ArtifactMetadataSource, org.apache.maven.artifact.resolver.filter.ArtifactFilter)
     */
    public ArtifactResolutionResult resolveTransitively( Set<Artifact> artifacts, Artifact originatingArtifact,
                                                         ArtifactRepository localRepository,
                                                         List<ArtifactRepository> remoteRepositories,
                                                         ArtifactMetadataSource source, ArtifactFilter filter )
        throws ArtifactResolutionException, ArtifactNotFoundException
    {
        return null;
    }

    /**
     * @return <code>null</code>.
     * @see org.apache.maven.artifact.resolver.ArtifactResolver#resolveTransitively(java.util.Set, org.apache.maven.artifact.Artifact, java.util.Map, org.apache.maven.artifact.repository.ArtifactRepository, java.util.List, org.apache.maven.artifact.metadata.ArtifactMetadataSource)
     */
    public ArtifactResolutionResult resolveTransitively( Set<Artifact> artifacts, Artifact originatingArtifact,
                                                         Map managedVersions, ArtifactRepository localRepository,
                                                         List<ArtifactRepository> remoteRepositories,
                                                         ArtifactMetadataSource source )
        throws ArtifactResolutionException, ArtifactNotFoundException
    {
        return null;
    }

    /**
     * @return <code>null</code>.
     * @see org.apache.maven.artifact.resolver.ArtifactResolver#resolveTransitively(java.util.Set, org.apache.maven.artifact.Artifact, java.util.Map, org.apache.maven.artifact.repository.ArtifactRepository, java.util.List, org.apache.maven.artifact.metadata.ArtifactMetadataSource, org.apache.maven.artifact.resolver.filter.ArtifactFilter)
     */
    public ArtifactResolutionResult resolveTransitively( Set<Artifact> artifacts, Artifact originatingArtifact,
                                                         Map managedVersions, ArtifactRepository localRepository,
                                                         List<ArtifactRepository> remoteRepositories,
                                                         ArtifactMetadataSource source, ArtifactFilter filter )
        throws ArtifactResolutionException, ArtifactNotFoundException
    {
        return null;
    }

    /**
     * @return <code>null</code>.
     * @see org.apache.maven.artifact.resolver.ArtifactResolver#resolveTransitively(java.util.Set, org.apache.maven.artifact.Artifact, java.util.Map, org.apache.maven.artifact.repository.ArtifactRepository, java.util.List, org.apache.maven.artifact.metadata.ArtifactMetadataSource, org.apache.maven.artifact.resolver.filter.ArtifactFilter, java.util.List)
     */
    public ArtifactResolutionResult resolveTransitively( Set<Artifact> artifacts, Artifact originatingArtifact,
                                                         Map managedVersions, ArtifactRepository localRepository,
                                                         List<ArtifactRepository> remoteRepositories,
                                                         ArtifactMetadataSource source, ArtifactFilter filter,
                                                         List<ResolutionListener> listeners )
        throws ArtifactResolutionException, ArtifactNotFoundException
    {
        return null;
    }

    /**
     * By default, do nothing.
     *
     * @see org.apache.maven.artifact.resolver.ArtifactResolver#resolveAlways(org.apache.maven.artifact.Artifact, java.util.List, org.apache.maven.artifact.repository.ArtifactRepository)
     */
    public void resolveAlways( Artifact artifact, List<ArtifactRepository> remoteRepositories,
                               ArtifactRepository localRepository )
        throws ArtifactResolutionException, ArtifactNotFoundException
    {
        // nop
    }

    public void resolve( Artifact artifact, List<ArtifactRepository> remoteRepositories,
                         ArtifactRepository localRepository, TransferListener downloadMonitor )
        throws ArtifactResolutionException, ArtifactNotFoundException
    {
        // TODO Auto-generated method stub
        
    }
    
    public ArtifactResolutionResult collect( ArtifactResolutionRequest request )
    {
        return null;
    }

    public ArtifactResolutionResult resolve( ArtifactResolutionRequest request )
    {
        return null;
    }
}
