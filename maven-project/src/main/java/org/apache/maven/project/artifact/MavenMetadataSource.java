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
import org.apache.maven.artifact.resolver.filter.AndArtifactFilter;
import org.apache.maven.artifact.resolver.filter.ArtifactFilter;
import org.apache.maven.artifact.resolver.filter.ExcludesArtifactFilter;
import org.apache.maven.artifact.versioning.InvalidVersionSpecificationException;
import org.apache.maven.artifact.versioning.VersionRange;
import org.apache.maven.model.Dependency;
import org.apache.maven.model.Exclusion;
import org.apache.maven.model.Relocation;
import org.apache.maven.project.MavenProject;
import org.apache.maven.project.MavenProjectBuilder;
import org.apache.maven.project.ProjectBuildingException;
import org.codehaus.plexus.logging.AbstractLogEnabled;

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
    extends AbstractLogEnabled
    implements ArtifactMetadataSource
{
    private MavenProjectBuilder mavenProjectBuilder;

    private ArtifactFactory artifactFactory;

    /**
     * Retrieve the metadata for the project from the repository.
     * Uses the ProjectBuilder, to enable post-processing and inheritance calculation before retrieving the
     * associated artifacts.
     */
    public ResolutionGroup retrieve( Artifact artifact, ArtifactRepository localRepository, List remoteRepositories )
        throws ArtifactMetadataRetrievalException
    {
        MavenProject p;

        Artifact pomArtifact;
        boolean done = false;
        do
        {
            // TODO: can we just modify the original?
            pomArtifact = artifactFactory.createProjectArtifact( artifact.getGroupId(), artifact.getArtifactId(),
                                                                 artifact.getVersion(), artifact.getScope() );

            try
            {
                p = mavenProjectBuilder.buildFromRepository( pomArtifact, remoteRepositories, localRepository );
            }
            catch ( ProjectBuildingException e )
            {
                throw new ArtifactMetadataRetrievalException( "Unable to read the metadata file", e );
            }

            Relocation relocation = null;

            if ( p.getDistributionManagement() != null )
            {
                relocation = p.getDistributionManagement().getRelocation();
            }
            if ( relocation != null )
            {
                if ( relocation.getGroupId() != null )
                {
                    artifact.setGroupId( relocation.getGroupId() );
                }
                if ( relocation.getArtifactId() != null )
                {
                    artifact.setArtifactId( relocation.getArtifactId() );
                }
                if ( relocation.getVersion() != null )
                {
                    artifact.setVersion( relocation.getVersion() );
                }

                String message = "\n  This artifact has been relocated to " + artifact.getGroupId() + ":" +
                    artifact.getArtifactId() + ":" + artifact.getVersion() + ".\n";

                if ( relocation.getMessage() != null )
                {
                    message += "  " + relocation.getMessage() + "\n";
                }

                getLogger().warn( message + "\n" );
            }
            else
            {
                done = true;
            }
        }
        while ( !done );

        artifact.setDownloadUrl( pomArtifact.getDownloadUrl() );

        try
        {
            // TODO: we could possibly use p.getDependencyArtifacts instead, but they haven't been filtered or used the
            // scope (should that be passed to the buildFromRepository method above?
            Set artifacts = createArtifacts( artifactFactory, p.getDependencies(), artifact.getScope(),
                                             artifact.getDependencyFilter() );

            return new ResolutionGroup( artifacts, p.getRemoteArtifactRepositories() );
        }
        catch ( InvalidVersionSpecificationException e )
        {
            throw new ArtifactMetadataRetrievalException( "Unable to read the metadata file", e );
        }
    }

    public static Set createArtifacts( ArtifactFactory artifactFactory, List dependencies, String inheritedScope,
                                       ArtifactFilter dependencyFilter )
        throws InvalidVersionSpecificationException
    {
        Set projectArtifacts = new HashSet();

        for ( Iterator i = dependencies.iterator(); i.hasNext(); )
        {
            Dependency d = (Dependency) i.next();

            // TODO: validate
            VersionRange versionRange = VersionRange.createFromVersionSpec( d.getVersion() );
            Artifact artifact = artifactFactory.createDependencyArtifact( d.getGroupId(), d.getArtifactId(),
                                                                          versionRange, d.getType(), d.getScope(),
                                                                          inheritedScope );

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
