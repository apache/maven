package org.apache.maven.project;

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
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;

import org.apache.maven.MavenTools;
import org.apache.maven.artifact.Artifact;
import org.apache.maven.artifact.ArtifactUtils;
import org.apache.maven.artifact.InvalidRepositoryException;
import org.apache.maven.artifact.factory.ArtifactFactory;
import org.apache.maven.artifact.repository.ArtifactRepository;
import org.apache.maven.artifact.resolver.ArtifactNotFoundException;
import org.apache.maven.artifact.resolver.ArtifactResolutionException;
import org.apache.maven.artifact.resolver.ArtifactResolver;
import org.apache.maven.model.Model;
import org.apache.maven.model.Repository;
import org.apache.maven.model.io.xpp3.MavenXpp3Reader;
import org.apache.maven.profiles.build.ProfileAdvisor;
import org.codehaus.plexus.component.annotations.Component;
import org.codehaus.plexus.component.annotations.Requirement;
import org.codehaus.plexus.logging.LogEnabled;
import org.codehaus.plexus.logging.Logger;
import org.codehaus.plexus.personality.plexus.lifecycle.phase.Initializable;
import org.codehaus.plexus.personality.plexus.lifecycle.phase.InitializationException;
import org.codehaus.plexus.util.StringUtils;

/**
 * This is a temporary class. These methods are originally from the DefaultMavenProjectHelper. This class will be
 * eliminated when Mercury is integrated.
 */
@Component(role = RepositoryHelper.class)
public class DefaultRepositoryHelper
    implements RepositoryHelper, Initializable, LogEnabled
{
    @Requirement
    private ArtifactFactory artifactFactory;

    @Requirement
    private ArtifactResolver artifactResolver;

    @Requirement
    private MavenTools mavenTools;

    @Requirement
    private ProfileAdvisor profileAdvisor;
    
    private Logger logger;

    public static final String MAVEN_MODEL_VERSION = "4.0.0";
  
    private MavenXpp3Reader modelReader;

    private static HashMap<String, Artifact> cache = new HashMap<String, Artifact>();

    private Logger getLogger()
    {
        return logger;
    }

    public void findModelFromRepository( Artifact artifact, List remoteArtifactRepositories,
                                          ArtifactRepository localRepository )
        throws ProjectBuildingException
    {

        if(cache.containsKey(artifact.getId()))
        {
            artifact.setFile(cache.get(artifact.getId()).getFile());
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
            getLogger().debug( "Attempting to build MavenProject instance for Artifact (" + artifact.getGroupId() + ":" +
                artifact.getArtifactId() + ":" + artifact.getVersion() + ") of type: " + artifact.getType() +
                "; constructing POM artifact instead." );

            projectArtifact = artifactFactory.createProjectArtifact( artifact.getGroupId(), artifact.getArtifactId(),
                                                                     artifact.getVersion(), artifact.getScope() );
        }

        try
        {
            artifactResolver.resolve( projectArtifact, remoteArtifactRepositories, localRepository );

            File file = projectArtifact.getFile();
            artifact.setFile( file );
            cache.put(artifact.getId(), artifact);
        }
        catch ( ArtifactResolutionException e )
        {
            throw new ProjectBuildingException( projectId, "Error getting POM for '" + projectId +
                "' from the repository: " + e.getMessage(), e );
        }
        catch ( ArtifactNotFoundException e )
        {
            throw new ProjectBuildingException( projectId,
                                                "POM '" + projectId + "' not found in repository: " + e.getMessage(),
                                                e );
        }
    }

    public List buildArtifactRepositories( Model model )
        throws ProjectBuildingException
    {
        try
        {
            return mavenTools.buildArtifactRepositories( model.getRepositories() );
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
                    item = mavenTools.buildArtifactRepository( repo );

                    normalized.add( item );
                    normalizationNeeded = true;
                }
                catch ( InvalidRepositoryException e )
                {
                    throw new ProjectBuildingException( projectId,
                                                        "Error building artifact repository for id: " + repo.getId(),
                                                        e );
                }
            }
            else
            {
                throw new ProjectBuildingException( projectId,
                                                    "Error building artifact repository from non-repository information item: " +
                                                        item );
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

    public void initialize()
        throws InitializationException
    {
        modelReader = new MavenXpp3Reader();
    }

    public void enableLogging( Logger logger )
    {
        this.logger = logger;
    }
}