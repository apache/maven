/*
Licensed to the Apache Software Foundation (ASF) under one
or more contributor license agreements.  See the NOTICE file
distributed with this work for additional information
regarding copyright ownership.  The ASF licenses this file
to you under the Apache License, Version 2.0 (the
"License"); you may not use this file except in compliance
with the License.  You may obtain a copy of the License at

    http://www.apache.org/licenses/LICENSE-2.0

Unless required by applicable law or agreed to in writing,
software distributed under the License is distributed on an
"AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
KIND, either express or implied.  See the License for the
specific language governing permissions and limitations
under the License.
 */

package org.apache.maven.repository.mercury;

import java.io.File;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;

import org.apache.maven.artifact.Artifact;
import org.apache.maven.artifact.metadata.ArtifactMetadataRetrievalException;
import org.apache.maven.artifact.metadata.ResolutionGroup;
import org.apache.maven.artifact.repository.ArtifactRepository;
import org.apache.maven.artifact.resolver.ArtifactResolutionException;
import org.apache.maven.artifact.resolver.ArtifactResolutionRequest;
import org.apache.maven.artifact.resolver.ArtifactResolutionResult;
import org.apache.maven.mercury.artifact.ArtifactMetadata;
import org.apache.maven.mercury.builder.api.DependencyProcessor;
import org.apache.maven.mercury.plexus.PlexusMercury;
import org.apache.maven.mercury.repository.api.Repository;
import org.apache.maven.mercury.repository.api.RepositoryException;
import org.apache.maven.mercury.repository.local.m2.LocalRepositoryM2;
import org.apache.maven.mercury.repository.remote.m2.RemoteRepositoryM2;
import org.apache.maven.mercury.transport.api.Server;
import org.apache.maven.mercury.util.Util;
import org.apache.maven.repository.LegacyMavenRepositorySystem;
import org.apache.maven.repository.MavenRepositorySystem;
import org.codehaus.plexus.component.annotations.Component;
import org.codehaus.plexus.component.annotations.Requirement;
import org.codehaus.plexus.lang.DefaultLanguage;
import org.codehaus.plexus.lang.Language;

/**
 * @author Oleg Gusakov
 * @version $Id$
 */
@Component( role = MavenRepositorySystem.class, hint = "mercury" )
public class MercuryRepositorySystem
    extends LegacyMavenRepositorySystem
    implements MavenRepositorySystem
{
    private static final Language LANG = new DefaultLanguage( MercuryRepositorySystem.class );

    @Requirement( hint = "maven" )
    DependencyProcessor _dependencyProcessor;

    @Requirement
    PlexusMercury _mercury;

    @Override
    public ArtifactResolutionResult resolve( ArtifactResolutionRequest request )
    {
        if ( request == null )
            throw new IllegalArgumentException( LANG.getMessage( "null.request" ) );

        if ( request.getArtifact() == null )
            throw new IllegalArgumentException( LANG.getMessage( "null.request.artifact" ) );

        ArtifactResolutionResult result = new ArtifactResolutionResult();

        List<Repository> repos =
            MercuryAdaptor.toMercuryRepos( request.getLocalRepository(), request.getRemoteRepostories(),
                                           _dependencyProcessor );

        try
        {
            List<ArtifactMetadata> mercuryMetadataList =
                _mercury.resolve( repos, null, MercuryAdaptor.toMercuryMetadata( request.getArtifact() ) );

            List<org.apache.maven.mercury.artifact.Artifact> mercuryArtifactList =
                _mercury.read( repos, mercuryMetadataList );

            if ( !Util.isEmpty( mercuryArtifactList ) )
                for ( org.apache.maven.mercury.artifact.Artifact a : mercuryArtifactList )
                    result.addArtifact( MercuryAdaptor.toMavenArtifact( a ) );
        }
        catch ( RepositoryException e )
        {
            result.addErrorArtifactException( new ArtifactResolutionException( e.getMessage(), request.getArtifact(),
                                                                               request.getRemoteRepostories() ) );
        }

        return result;
    }

    @Override
    public ResolutionGroup retrieve( Artifact artifact, ArtifactRepository localRepository,
                                     List<ArtifactRepository> remoteRepositories )
        throws ArtifactMetadataRetrievalException
    {
        // TODO Auto-generated method stub
        return super.retrieve( artifact, localRepository, remoteRepositories );
    }

}
