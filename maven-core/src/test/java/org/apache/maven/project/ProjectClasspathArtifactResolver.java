package org.apache.maven.project;

/*
 * Copyright 2001-2005 The Apache Software Foundation.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

import org.apache.maven.artifact.Artifact;
import org.apache.maven.artifact.MavenMetadataSource;
import org.apache.maven.artifact.factory.ArtifactFactory;
import org.apache.maven.artifact.factory.DefaultArtifactFactory;
import org.apache.maven.artifact.metadata.ArtifactMetadataRetrievalException;
import org.apache.maven.artifact.metadata.ArtifactMetadataSource;
import org.apache.maven.artifact.repository.ArtifactRepository;
import org.apache.maven.artifact.resolver.ArtifactResolutionException;
import org.apache.maven.artifact.resolver.ArtifactResolutionResult;
import org.apache.maven.artifact.resolver.ArtifactResolver;
import org.apache.maven.artifact.resolver.DefaultArtifactResolver;
import org.apache.maven.artifact.resolver.filter.ArtifactFilter;
import org.apache.maven.model.Model;
import org.apache.maven.model.io.xpp3.MavenXpp3Reader;

import java.io.File;
import java.io.InputStreamReader;
import java.util.List;
import java.util.Set;

public class ProjectClasspathArtifactResolver
    extends DefaultArtifactResolver
{
    private static class Source
        extends MavenMetadataSource
    {
        private ArtifactFactory artifactFactory = new DefaultArtifactFactory();

        public Source( ArtifactResolver artifactResolver )
        {
            super( artifactResolver );
        }

        public Set retrieve( Artifact artifact, ArtifactRepository localRepository, List remoteRepositories )
            throws ArtifactMetadataRetrievalException
        {
            MavenXpp3Reader reader = new MavenXpp3Reader();
            Model model = null;
            try
            {
                String scope = artifact.getArtifactId().substring( "scope-".length() );
                String name = "/projects/scope/transitive-" + scope + "-dep.xml";
                model = reader.read( new InputStreamReader( getClass().getResourceAsStream( name ) ) );
            }
            catch ( Exception e )
            {
                throw new ArtifactMetadataRetrievalException( e );
            }
            return artifactFactory.createArtifacts( model.getDependencies(), localRepository, artifact.getScope() );
        }
    }

    public Artifact resolve( Artifact artifact, List remoteRepositories, ArtifactRepository localRepository )
        throws ArtifactResolutionException
    {
        artifact.setFile( new File( "dummy" ) );
        return artifact;
    }

    public ArtifactResolutionResult resolveTransitively( Set artifacts, List remoteRepositories,
                                                         ArtifactRepository localRepository,
                                                         ArtifactMetadataSource source, ArtifactFilter filter )
        throws ArtifactResolutionException
    {
        return super.resolveTransitively( artifacts, remoteRepositories, localRepository, new Source( this ), filter );
    }

    public ArtifactResolutionResult resolveTransitively( Set artifacts, List remoteRepositories,
                                                         ArtifactRepository localRepository,
                                                         ArtifactMetadataSource source )
        throws ArtifactResolutionException
    {
        return super.resolveTransitively( artifacts, remoteRepositories, localRepository, new Source( this ) );
    }

    public ArtifactResolutionResult resolveTransitively( Artifact artifact, List remoteRepositories,
                                                         ArtifactRepository localRepository,
                                                         ArtifactMetadataSource source )
        throws ArtifactResolutionException
    {
        return super.resolveTransitively( artifact, remoteRepositories, localRepository, new Source( this ) );
    }
}