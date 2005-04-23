package org.apache.maven.project.artifact;

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

import org.apache.maven.artifact.factory.ArtifactFactory;
import org.apache.maven.artifact.factory.DefaultArtifactFactory;
import org.apache.maven.artifact.metadata.ArtifactMetadataRetrievalException;
import org.apache.maven.artifact.metadata.ArtifactMetadataSource;
import org.apache.maven.artifact.repository.ArtifactRepository;
import org.apache.maven.artifact.resolver.ArtifactResolutionException;
import org.apache.maven.artifact.resolver.ArtifactResolver;
import org.apache.maven.artifact.Artifact;
import org.apache.maven.model.Dependency;
import org.apache.maven.model.Model;
import org.apache.maven.model.io.xpp3.MavenXpp3Reader;
import org.apache.maven.project.MavenProject;
import org.apache.maven.project.MavenProjectBuilder;
import org.apache.maven.project.ProjectBuildingException;
import org.apache.maven.wagon.util.IoUtils;
import org.codehaus.plexus.util.xml.pull.XmlPullParserException;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Set;

/**
 * @author <a href="mailto:jason@maven.org">Jason van Zyl </a>
 * @version $Id$
 */
public class MavenMetadataSource
    implements ArtifactMetadataSource
{
    private MavenProjectBuilder mavenProjectBuilder;

    private ArtifactResolver artifactResolver;

    // TODO: configure?
    protected ArtifactFactory artifactFactory = new DefaultArtifactFactory();

    /**
     * @todo remove.
     */
    private MavenXpp3Reader reader = new MavenXpp3Reader();

    public MavenMetadataSource( ArtifactResolver artifactResolver )
    {
        this.artifactResolver = artifactResolver;
        this.mavenProjectBuilder = null;
    }

    public MavenMetadataSource( ArtifactResolver artifactResolver, MavenProjectBuilder projectBuilder )
    {
        this.artifactResolver = artifactResolver;
        this.mavenProjectBuilder = projectBuilder;
    }

    public Set retrieve( Artifact artifact, ArtifactRepository localRepository, List remoteRepositories )
        throws ArtifactMetadataRetrievalException
    {
        // TODO: only metadata is really needed - resolve as metadata
        artifact = artifactFactory.createArtifact( artifact.getGroupId(), artifact.getArtifactId(),
                                                   artifact.getVersion(), artifact.getScope(), "pom" );

        List dependencies = null;

        // Use the ProjectBuilder, to enable post-processing and inheritance calculation before retrieving the
        // associated artifacts.
        if ( mavenProjectBuilder != null )
        {
            try
            {
                MavenProject p = mavenProjectBuilder.buildFromRepository( artifact, remoteRepositories,
                                                                          localRepository );
                dependencies = p.getDependencies();
            }
            catch ( ProjectBuildingException e )
            {
                throw new ArtifactMetadataRetrievalException( "Unable to read the metadata file", e );
            }
        }
        else
        {
            // there is code in plexus that uses this (though it shouldn't) so we
            // need to be able to not have a project builder
            // TODO: remove - which then makes this a very thin wrapper around a project builder - is it needed?

            try
            {
                artifactResolver.resolve( artifact, remoteRepositories, localRepository );
            }
            catch ( ArtifactResolutionException e )
            {
                throw new ArtifactMetadataRetrievalException( "Error while resolving metadata artifact", e );
            }

            FileReader reader = null;
            try
            {
//                String path = localRepository.pathOfMetadata( new ProjectArtifactMetadata( artifact, null ) );
//                File file = new File( localRepository.getBasedir(), path );
                File file = artifact.getFile();
                reader = new FileReader( file );
                Model model = this.reader.read( reader );
                dependencies = model.getDependencies();
            }
            catch ( FileNotFoundException e )
            {
                throw new ArtifactMetadataRetrievalException( "Unable to find the metadata file", e );
            }
            catch ( IOException e )
            {
                throw new ArtifactMetadataRetrievalException( "Unable to read the metadata file", e );
            }
            catch ( XmlPullParserException e )
            {
                throw new ArtifactMetadataRetrievalException( "Unable to parse the metadata file", e );
            }
            finally
            {
                IoUtils.close( reader );
            }
        }
        return createArtifacts( dependencies, artifact.getScope() );
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
