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
import org.apache.maven.artifact.factory.ArtifactFactory;
import org.apache.maven.artifact.metadata.ArtifactMetadataRetrievalException;
import org.apache.maven.artifact.metadata.ArtifactMetadataSource;
import org.apache.maven.artifact.repository.ArtifactRepository;
import org.apache.maven.artifact.resolver.ArtifactResolutionException;
import org.apache.maven.artifact.resolver.ArtifactResolutionResult;
import org.apache.maven.artifact.resolver.DefaultArtifactResolver;
import org.apache.maven.artifact.resolver.filter.ArtifactFilter;
import org.apache.maven.model.Dependency;
import org.apache.maven.model.Model;
import org.apache.maven.model.io.xpp3.MavenXpp3Reader;
import org.codehaus.plexus.util.IOUtil;
import org.codehaus.plexus.util.xml.pull.XmlPullParserException;

import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Set;

public class ProjectClasspathArtifactResolver
    extends DefaultArtifactResolver
{
    public static class Source
        implements ArtifactMetadataSource
    {
        private ArtifactFactory artifactFactory;

        public Source( ArtifactFactory artifactFactory )
        {
            this.artifactFactory = artifactFactory;
        }

        public Set retrieve( Artifact artifact, ArtifactRepository localRepository, List remoteRepositories )
            throws ArtifactMetadataRetrievalException
        {
            Model model = null;
            InputStreamReader r = null;
            try
            {
                String scope = artifact.getArtifactId().substring( "scope-".length() );
                String name = "/projects/scope/transitive-" + scope + "-dep.xml";
                r = new InputStreamReader( getClass().getResourceAsStream( name ) );
                MavenXpp3Reader reader = new MavenXpp3Reader();
                model = reader.read( r );
            }
            catch ( IOException e )
            {
                throw new ArtifactMetadataRetrievalException( e );
            }
            catch ( XmlPullParserException e )
            {
                throw new ArtifactMetadataRetrievalException( e );
            }
            finally
            {
                IOUtil.close( r );
            }
            return createArtifacts( model.getDependencies(), artifact.getScope() );
        }

        protected Set createArtifacts( List dependencies, String inheritedScope )
        {
            Set projectArtifacts = new HashSet();

            for ( Iterator i = dependencies.iterator(); i.hasNext(); )
            {
                Dependency d = (Dependency) i.next();

                Artifact artifact = artifactFactory.createArtifact( d.getGroupId(), d.getArtifactId(), d.getVersion(),
                                                                    d.getScope(), d.getType(), inheritedScope );
                if ( artifact != null )
                {
                    projectArtifacts.add( artifact );
                }
            }

            return projectArtifacts;
        }
    }

    public void resolve( Artifact artifact, List remoteRepositories, ArtifactRepository localRepository )
        throws ArtifactResolutionException
    {
        artifact.setFile( new File( "dummy" ) );
    }

    public ArtifactResolutionResult resolveTransitively( Set artifacts, List remoteRepositories,
                                                         ArtifactRepository localRepository,
                                                         ArtifactMetadataSource source, ArtifactFilter filter )
        throws ArtifactResolutionException
    {
        return super.resolveTransitively( artifacts, remoteRepositories, localRepository, new Source( artifactFactory ),
                                          filter );
    }

    public ArtifactResolutionResult resolveTransitively( Set artifacts, List remoteRepositories,
                                                         ArtifactRepository localRepository,
                                                         ArtifactMetadataSource source )
        throws ArtifactResolutionException
    {
        return super.resolveTransitively( artifacts, remoteRepositories, localRepository,
                                          new Source( artifactFactory ) );
    }

    public ArtifactResolutionResult resolveTransitively( Artifact artifact, List remoteRepositories,
                                                         ArtifactRepository localRepository,
                                                         ArtifactMetadataSource source )
        throws ArtifactResolutionException
    {
        return super.resolveTransitively( artifact, remoteRepositories, localRepository,
                                          new Source( artifactFactory ) );
    }
}