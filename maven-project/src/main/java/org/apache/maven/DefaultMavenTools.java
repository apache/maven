package org.apache.maven;

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

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;

import org.apache.maven.artifact.Artifact;
import org.apache.maven.artifact.ArtifactUtils;
import org.apache.maven.artifact.InvalidRepositoryException;
import org.apache.maven.artifact.factory.ArtifactFactory;
import org.apache.maven.artifact.repository.ArtifactRepository;
import org.apache.maven.artifact.repository.ArtifactRepositoryFactory;
import org.apache.maven.artifact.repository.ArtifactRepositoryPolicy;
import org.apache.maven.artifact.repository.layout.ArtifactRepositoryLayout;
import org.apache.maven.artifact.resolver.ArtifactNotFoundException;
import org.apache.maven.artifact.resolver.ArtifactResolutionException;
import org.apache.maven.artifact.resolver.ArtifactResolver;
import org.apache.maven.model.DeploymentRepository;
import org.apache.maven.model.Model;
import org.apache.maven.model.Repository;
import org.apache.maven.model.RepositoryPolicy;
import org.apache.maven.project.MissingRepositoryElementException;
import org.apache.maven.project.ProjectBuildingException;
import org.codehaus.plexus.component.annotations.Component;
import org.codehaus.plexus.component.annotations.Requirement;
import org.codehaus.plexus.logging.LogEnabled;
import org.codehaus.plexus.logging.Logger;
import org.codehaus.plexus.util.StringUtils;

/**
 * @author Jason van Zyl
 */
@Component(role = MavenTools.class)
public class DefaultMavenTools
    implements MavenTools, LogEnabled
{
    @Requirement
    private ArtifactFactory artifactFactory;

    @Requirement
    private ArtifactResolver artifactResolver;
    
    @Requirement
    private ArtifactRepositoryFactory artifactRepositoryFactory;

    @Requirement
    private ArtifactRepositoryLayout defaultArtifactRepositoryLayout;
        
    @Requirement
    private Logger logger;
    
    private static HashMap<String, Artifact> cache = new HashMap<String, Artifact>();
    
    // ----------------------------------------------------------------------------
    // Code snagged from ProjectUtils: this will have to be moved somewhere else
    // but just trying to collect it all in one place right now.
    // ----------------------------------------------------------------------------

    public List<ArtifactRepository> buildArtifactRepositories( List<Repository> repositories )
        throws InvalidRepositoryException
    {
        List<ArtifactRepository> repos = new ArrayList<ArtifactRepository>();

        for( Repository mavenRepo : repositories )
        {
            ArtifactRepository artifactRepo = buildArtifactRepository( mavenRepo );

            if ( !repos.contains( artifactRepo ) )
            {
                repos.add( artifactRepo );
            }
        }
        
        return repos;
    }

    public ArtifactRepository buildDeploymentArtifactRepository( DeploymentRepository repo )
        throws InvalidRepositoryException
    {
        if ( repo != null )
        {
            String id = repo.getId();
            String url = repo.getUrl();

            return artifactRepositoryFactory.createDeploymentArtifactRepository( id, url, repo.getLayout(), repo.isUniqueVersion() );
        }
        else
        {
            return null;
        }
    }

    public ArtifactRepository buildArtifactRepository( Repository repo )
        throws InvalidRepositoryException
    {
        if ( repo != null )
        {
            String id = repo.getId();
            String url = repo.getUrl();

            if ( id == null || id.trim().length() < 1 )
            {
                throw new InvalidRepositoryException( "Repository ID must not be empty (URL is: " + url + ").", url );
            }

            if ( url == null || url.trim().length() < 1 )                
            {
                throw new InvalidRepositoryException( "Repository URL must not be empty (ID is: " + id + ").", id );
            }

            ArtifactRepositoryPolicy snapshots = buildArtifactRepositoryPolicy( repo.getSnapshots() );

            ArtifactRepositoryPolicy releases = buildArtifactRepositoryPolicy( repo.getReleases() );

            return artifactRepositoryFactory.createArtifactRepository( id, url, repo.getLayout(), snapshots, releases );
        }
        else
        {
            return null;
        }
    }

    public ArtifactRepositoryPolicy buildArtifactRepositoryPolicy( RepositoryPolicy policy )
    {
        boolean enabled = true;

        String updatePolicy = null;

        String checksumPolicy = null;

        if ( policy != null )
        {
            enabled = policy.isEnabled();

            if ( policy.getUpdatePolicy() != null )
            {
                updatePolicy = policy.getUpdatePolicy();
            }
            if ( policy.getChecksumPolicy() != null )
            {
                checksumPolicy = policy.getChecksumPolicy();
            }
        }

        return new ArtifactRepositoryPolicy( enabled, updatePolicy, checksumPolicy );
    }
    
    // From MavenExecutionRequestPopulator

    public ArtifactRepository createLocalRepository( String url, String repositoryId )
        throws IOException
    {
        return createRepository( canonicalFileUrl( url ), repositoryId );
    }

    private String canonicalFileUrl( String url )
        throws IOException
    {
        if ( !url.startsWith( "file:" ) )
        {
            url = "file://" + url;
        }
        else if ( url.startsWith( "file:" ) && !url.startsWith( "file://" ) )
        {
            url = "file://" + url.substring( "file:".length() );
        }

        // So now we have an url of the form file://<path>

        // We want to eliminate any relative path nonsense and lock down the path so we
        // need to fully resolve it before any sub-modules use the path. This can happen
        // when you are using a custom settings.xml that contains a relative path entry
        // for the local repository setting.

        File localRepository = new File( url.substring( "file://".length() ) );

        if ( !localRepository.isAbsolute() )
        {
            url = "file://" + localRepository.getCanonicalPath();
        }

        return url;
    }

    public ArtifactRepository createRepository( String url, String repositoryId )
    {
        // snapshots vs releases
        // offline = to turning the update policy off

        //TODO: we'll need to allow finer grained creation of repositories but this will do for now

        String updatePolicyFlag = ArtifactRepositoryPolicy.UPDATE_POLICY_ALWAYS;

        String checksumPolicyFlag = ArtifactRepositoryPolicy.CHECKSUM_POLICY_WARN;

        ArtifactRepositoryPolicy snapshotsPolicy = new ArtifactRepositoryPolicy( true, updatePolicyFlag, checksumPolicyFlag );

        ArtifactRepositoryPolicy releasesPolicy = new ArtifactRepositoryPolicy( true, updatePolicyFlag, checksumPolicyFlag );

        return artifactRepositoryFactory.createArtifactRepository( repositoryId, url, defaultArtifactRepositoryLayout, snapshotsPolicy, releasesPolicy );
    }
    
    public ArtifactRepository createRepository( String url,
                                                String repositoryId,
                                                ArtifactRepositoryPolicy snapshotsPolicy,
                                                ArtifactRepositoryPolicy releasesPolicy )
    {
        return artifactRepositoryFactory.createArtifactRepository( repositoryId, url, defaultArtifactRepositoryLayout, snapshotsPolicy, releasesPolicy );        
    }

    public void setGlobalUpdatePolicy( String policy )
    {
        artifactRepositoryFactory.setGlobalUpdatePolicy( policy );
    }

    public void setGlobalChecksumPolicy( String policy )
    {
        artifactRepositoryFactory.setGlobalChecksumPolicy( policy );        
    }
    
    // Taken from RepositoryHelper
    
    public void findModelFromRepository( Artifact artifact, List remoteArtifactRepositories, ArtifactRepository localRepository )
        throws ProjectBuildingException
    {

        if ( cache.containsKey( artifact.getId() ) )
        {
            artifact.setFile( cache.get( artifact.getId() ).getFile() );
        }

        String projectId = safeVersionlessKey( artifact.getGroupId(), artifact.getArtifactId() );
        remoteArtifactRepositories = normalizeToArtifactRepositories( remoteArtifactRepositories, projectId );

        Artifact projectArtifact;

        // if the artifact is not a POM, we need to construct a POM artifact based on the artifact parameter given.
        if ( "pom".equals( artifact.getType() ) )
        {
            projectArtifact = artifact;
        }
        else
        {
            logger.debug( "Attempting to build MavenProject instance for Artifact (" + artifact.getGroupId() + ":" + artifact.getArtifactId() + ":" + artifact.getVersion() + ") of type: "
                          + artifact.getType() + "; constructing POM artifact instead." );

            projectArtifact = artifactFactory.createProjectArtifact( artifact.getGroupId(), artifact.getArtifactId(), artifact.getVersion(), artifact.getScope() );
        }

        try
        {
            artifactResolver.resolve( projectArtifact, remoteArtifactRepositories, localRepository );

            File file = projectArtifact.getFile();
            artifact.setFile( file );
            cache.put( artifact.getId(), artifact );
        }
        catch ( ArtifactResolutionException e )
        {
            throw new ProjectBuildingException( projectId, "Error getting POM for '" + projectId + "' from the repository: " + e.getMessage(), e );
        }
        catch ( ArtifactNotFoundException e )
        {
            throw new ProjectBuildingException( projectId, "POM '" + projectId + "' not found in repository: " + e.getMessage(), e );
        }
    }

    public List<ArtifactRepository> buildArtifactRepositories( Model model )
        throws ProjectBuildingException
    {
        try
        {
            return buildArtifactRepositories( model.getRepositories() );
        }
        catch ( InvalidRepositoryException e )
        {
            String projectId = safeVersionlessKey( model.getGroupId(), model.getArtifactId() );

            throw new ProjectBuildingException( projectId, e.getMessage(), e );
        }
    }

    private List normalizeToArtifactRepositories( List remoteArtifactRepositories, String projectId )
        throws ProjectBuildingException
    {
        List normalized = new ArrayList( remoteArtifactRepositories.size() );

        boolean normalizationNeeded = false;
        for ( Iterator it = remoteArtifactRepositories.iterator(); it.hasNext(); )
        {
            Object item = it.next();

            if ( item instanceof ArtifactRepository )
            {
                normalized.add( item );
            }
            else if ( item instanceof Repository )
            {
                Repository repo = (Repository) item;
                try
                {
                    item = buildArtifactRepository( repo );

                    normalized.add( item );
                    normalizationNeeded = true;
                }
                catch ( InvalidRepositoryException e )
                {
                    throw new ProjectBuildingException( projectId, "Error building artifact repository for id: " + repo.getId(), e );
                }
            }
            else
            {
                throw new ProjectBuildingException( projectId, "Error building artifact repository from non-repository information item: " + item );
            }
        }

        if ( normalizationNeeded )
        {
            return normalized;
        }
        else
        {
            return remoteArtifactRepositories;
        }
    }

    private String safeVersionlessKey( String groupId, String artifactId )
    {
        String gid = groupId;

        if ( StringUtils.isEmpty( gid ) )
        {
            gid = "unknown";
        }

        String aid = artifactId;

        if ( StringUtils.isEmpty( aid ) )
        {
            aid = "unknown";
        }

        return ArtifactUtils.versionlessKey( gid, aid );
    }

    public void enableLogging( Logger logger )
    {
        this.logger = logger;
    }
    
    /**
     * Resolves the specified artifact
     *
     * @param artifact the artifact to resolve
     * @throws IOException if there is a problem resolving the artifact
     */
    public void resolve( Artifact artifact, ArtifactRepository localRepository, List<ArtifactRepository> remoteRepositories )
        throws IOException
    {
        File artifactFile = new File( localRepository.getBasedir(), localRepository.pathOf( artifact ) );
        artifact.setFile( artifactFile );

        try
        {
            artifactResolver.resolve( artifact, remoteRepositories, localRepository );
        }
        catch ( ArtifactResolutionException e )
        {
            throw new IOException( e.getMessage() );
        }
        catch ( ArtifactNotFoundException e )
        {
            throw new IOException( e.getMessage() );
        }
    }    
}
