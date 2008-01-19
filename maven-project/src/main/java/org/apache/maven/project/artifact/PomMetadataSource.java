package org.apache.maven.project.artifact;

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

import org.apache.maven.artifact.Artifact;
import org.apache.maven.artifact.factory.ArtifactFactory;
import org.apache.maven.artifact.metadata.ArtifactMetadataRetrievalException;
import org.apache.maven.artifact.repository.ArtifactRepository;
import org.apache.maven.artifact.resolver.metadata.ArtifactMetadata;
import org.apache.maven.artifact.resolver.metadata.MetadataResolution;
import org.apache.maven.artifact.resolver.metadata.MetadataRetrievalException;
import org.apache.maven.artifact.resolver.metadata.MetadataSource;
import org.apache.maven.model.Dependency;
import org.apache.maven.project.InvalidProjectModelException;
import org.apache.maven.project.MavenProject;
import org.apache.maven.project.MavenProjectBuilder;
import org.apache.maven.project.ProjectBuildingException;
import org.codehaus.plexus.PlexusConstants;
import org.codehaus.plexus.PlexusContainer;
import org.codehaus.plexus.component.repository.exception.ComponentLookupException;
import org.codehaus.plexus.context.Context;
import org.codehaus.plexus.context.ContextException;
import org.codehaus.plexus.logging.AbstractLogEnabled;
import org.codehaus.plexus.personality.plexus.lifecycle.phase.Contextualizable;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Set;

/**
 * @author Jason van Zyl
 * @version $Id$
 */
public class PomMetadataSource
    extends AbstractLogEnabled
    implements MetadataSource,
    Contextualizable
{
    public static final String ROLE_HINT = "default";

    private MavenProjectBuilder mavenProjectBuilder;

    private ArtifactFactory artifactFactory;

    // lazily instantiated and cached.
    private MavenProject superProject;

    private PlexusContainer container;

    /** Unfortunately we have projects that are still sending us JARs without the accompanying POMs. */
    private boolean strictlyEnforceThePresenceOfAValidMavenPOM = true;

    public MetadataResolution retrieve( ArtifactMetadata artifactMetadata,
                                        ArtifactRepository localRepository,
                                        List remoteRepositories )
        throws MetadataRetrievalException
    {
        try
        {
            loadProjectBuilder();
        }
        catch ( ComponentLookupException e )
        {
            throw new MetadataRetrievalException(
                "Cannot lookup MavenProjectBuilder component instance: " + e.getMessage(), e );
        }

        MavenProject project = null;

        Artifact pomArtifact = artifactFactory.createProjectArtifact(
            artifactMetadata.getGroupId()
            , artifactMetadata.getArtifactId()
            , artifactMetadata.getVersion()
        );

        try
        {
            project = mavenProjectBuilder.buildFromRepository( pomArtifact, remoteRepositories, localRepository );
            if ( pomArtifact.getFile() != null )
            {
                artifactMetadata.setArtifactUri( pomArtifact.getFile().toURI().toString() );
            }
        }
        catch ( InvalidProjectModelException e )
        {
            // We want to capture this in the graph so that we can display the error to the user
            artifactMetadata.setError( e.getMessage() );
        }
        catch ( ProjectBuildingException e )
        {
            if ( strictlyEnforceThePresenceOfAValidMavenPOM )
            {
                throw new MetadataRetrievalException(
                    "Unable to read the metadata file for artifactMetadata '" +
                        artifactMetadata.getDependencyConflictId() + "': " + e.getMessage(), e, artifactMetadata );
            }
        }

        Set artifacts = new HashSet();

        for ( Iterator i = project.getDependencies().iterator(); i.hasNext(); )
        {
            Dependency d = (Dependency) i.next();

            artifacts.add( new ArtifactMetadata( d.getGroupId(), d.getArtifactId(), d.getVersion(), d.getType(),
                d.getScope(), d.getClassifier(), null, null, false, null ) );
        }

        // The remote repositories is intentially null here while working in the graph in the least invasive way
        // and making sure repositories added for a POM are scope only for a particular POM. We don't want
        // repositories lingering around or being aggregated after they are used. jvz

        artifactMetadata.setDependencies( artifacts );

        return new MetadataResolution( artifactMetadata );
    }

    private void loadProjectBuilder()
        throws ComponentLookupException
    {
        if ( mavenProjectBuilder == null )
        {
            mavenProjectBuilder = (MavenProjectBuilder) container.lookup( MavenProjectBuilder.class );
        }
    }

    private List aggregateRepositoryLists( List remoteRepositories,
                                           List remoteArtifactRepositories )
        throws ArtifactMetadataRetrievalException
    {
        if ( superProject == null )
        {
            try
            {
                superProject = mavenProjectBuilder.buildStandaloneSuperProject();
            }
            catch ( ProjectBuildingException e )
            {
                throw new ArtifactMetadataRetrievalException(
                    "Unable to parse the Maven built-in model: " + e.getMessage(), e );
            }
        }

        List repositories = new ArrayList();

        repositories.addAll( remoteRepositories );

        // ensure that these are defined
        for ( Iterator it = superProject.getRemoteArtifactRepositories().iterator(); it.hasNext(); )
        {
            ArtifactRepository superRepo = (ArtifactRepository) it.next();

            for ( Iterator aggregatedIterator = repositories.iterator(); aggregatedIterator.hasNext(); )
            {
                ArtifactRepository repo = (ArtifactRepository) aggregatedIterator.next();

                // if the repository exists in the list and was introduced by another POM's super-pom,
                // remove it...the repository definitions from the super-POM should only be at the end of
                // the list.
                // if the repository has been redefined, leave it.
                if ( repo.getId().equals( superRepo.getId() ) && repo.getUrl().equals( superRepo.getUrl() ) )
                {
                    aggregatedIterator.remove();
                }
            }
        }

        // this list should contain the super-POM repositories, so we don't have to explicitly add them back.
        for ( Iterator it = remoteArtifactRepositories.iterator(); it.hasNext(); )
        {
            ArtifactRepository repository = (ArtifactRepository) it.next();

            if ( !repositories.contains( repository ) )
            {
                repositories.add( repository );
            }
        }

        return repositories;
    }

    public void contextualize( Context context )
        throws ContextException
    {
        container = (PlexusContainer) context.get( PlexusConstants.PLEXUS_KEY );
    }
}