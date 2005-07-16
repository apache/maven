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

import org.apache.maven.artifact.Artifact;
import org.apache.maven.artifact.factory.ArtifactFactory;
import org.apache.maven.artifact.metadata.ArtifactMetadataRetrievalException;
import org.apache.maven.artifact.metadata.ArtifactMetadataSource;
import org.apache.maven.artifact.metadata.ResolutionGroup;
import org.apache.maven.artifact.repository.ArtifactRepository;
import org.apache.maven.artifact.resolver.ArtifactResolver;
import org.apache.maven.artifact.resolver.filter.AndArtifactFilter;
import org.apache.maven.artifact.resolver.filter.ArtifactFilter;
import org.apache.maven.artifact.resolver.filter.ExcludesArtifactFilter;
import org.apache.maven.model.Dependency;
import org.apache.maven.model.Exclusion;
import org.apache.maven.project.MavenProject;
import org.apache.maven.project.MavenProjectBuilder;
import org.apache.maven.project.ProjectBuildingException;

import java.util.ArrayList;
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

    private ArtifactFactory artifactFactory;

    // TODO: Remove resolver from params list.
    public MavenMetadataSource( ArtifactResolver artifactResolver, MavenProjectBuilder projectBuilder,
                                ArtifactFactory artifactFactory )
    {
        this.mavenProjectBuilder = projectBuilder;
        this.artifactFactory = artifactFactory;
    }

    public ResolutionGroup retrieve( Artifact artifact, ArtifactRepository localRepository, List remoteRepositories )
        throws ArtifactMetadataRetrievalException
    {
        // TODO: only metadata is really needed - resolve as metadata
        Artifact pomArtifact = artifactFactory.createArtifact( artifact.getGroupId(), artifact.getArtifactId(),
                                                               artifact.getVersion(), artifact.getScope(), "pom" );

        // TODO: this a very thin wrapper around a project builder - is it needed?
        List dependencies = null;

        // Use the ProjectBuilder, to enable post-processing and inheritance calculation before retrieving the
        // associated artifacts.
        try
        {
            MavenProject p = mavenProjectBuilder.buildFromRepository( pomArtifact, remoteRepositories,
                                                                      localRepository );
            dependencies = p.getDependencies();
            artifact.setDownloadUrl( pomArtifact.getDownloadUrl() );
            
            Set artifacts = createArtifacts( artifactFactory, dependencies, artifact.getScope(), artifact.getDependencyFilter() );
            
            return new ResolutionGroup( artifacts, p.getRemoteArtifactRepositories() );
        }
        catch ( ProjectBuildingException e )
        {
            throw new ArtifactMetadataRetrievalException( "Unable to read the metadata file", e );
        }
    }

    public static Set createArtifacts( ArtifactFactory artifactFactory, List dependencies, String inheritedScope,
                                       ArtifactFilter dependencyFilter )
    {
        Set projectArtifacts = new HashSet();

        for ( Iterator i = dependencies.iterator(); i.hasNext(); )
        {
            Dependency d = (Dependency) i.next();

            Artifact artifact = artifactFactory.createArtifact( d.getGroupId(), d.getArtifactId(), d.getVersion(),
                                                                d.getScope(), d.getType(), inheritedScope );

            if ( artifact != null && ( dependencyFilter == null || dependencyFilter.include( artifact ) ) )
            {
                if ( d.getExclusions() != null && !d.getExclusions().isEmpty() )
                {
                    List exclusions = new ArrayList();
                    for ( Iterator j = d.getExclusions().iterator(); j.hasNext(); )
                    {
                        Exclusion e = (Exclusion) j.next();
                        exclusions.add( e.getGroupId() + ":" + e.getArtifactId() );
                    }

                    ArtifactFilter newFilter = new ExcludesArtifactFilter( exclusions );

                    if ( dependencyFilter != null )
                    {
                        AndArtifactFilter filter = new AndArtifactFilter();
                        filter.add( dependencyFilter );
                        filter.add( newFilter );
                        dependencyFilter = filter;
                    }
                    else
                    {
                        dependencyFilter = newFilter;
                    }
                }

                artifact.setDependencyFilter( dependencyFilter );

                projectArtifacts.add( artifact );
            }
        }

        return projectArtifacts;
    }
}
