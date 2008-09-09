package org.apache.maven.profiles.build;

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

import org.apache.maven.MavenTools;
import org.apache.maven.artifact.ArtifactUtils;
import org.apache.maven.artifact.InvalidRepositoryException;
import org.apache.maven.artifact.repository.ArtifactRepository;
import org.apache.maven.model.Model;
import org.apache.maven.model.Profile;
import org.apache.maven.model.Repository;
import org.apache.maven.profiles.DefaultProfileManager;
import org.apache.maven.profiles.MavenProfilesBuilder;
import org.apache.maven.profiles.ProfileManager;
import org.apache.maven.profiles.ProfilesConversionUtils;
import org.apache.maven.profiles.ProfilesRoot;
import org.apache.maven.profiles.activation.ProfileActivationContext;
import org.apache.maven.profiles.activation.ProfileActivationException;
import org.apache.maven.profiles.injection.ProfileInjector;
import org.apache.maven.project.ProjectBuildingException;
import org.codehaus.plexus.PlexusConstants;
import org.codehaus.plexus.PlexusContainer;
import org.codehaus.plexus.context.Context;
import org.codehaus.plexus.context.ContextException;
import org.codehaus.plexus.logging.LogEnabled;
import org.codehaus.plexus.logging.Logger;
import org.codehaus.plexus.personality.plexus.lifecycle.phase.Contextualizable;
import org.codehaus.plexus.util.xml.pull.XmlPullParserException;

import java.io.File;
import java.io.IOException;
import java.util.Collections;
import java.util.Iterator;
import java.util.LinkedHashSet;
import java.util.List;

public class DefaultProfileAdvisor
    implements ProfileAdvisor, LogEnabled, Contextualizable
{

    public static final String ROLE_HINT = "default";

    private MavenTools mavenTools;

    private MavenProfilesBuilder profilesBuilder;

    private ProfileInjector profileInjector;

    private PlexusContainer container;

    private Logger logger;

    public List applyActivatedProfiles( Model model, File pomFile, boolean useProfilesXml,
                                        ProfileActivationContext activationContext )
        throws ProjectBuildingException
    {
//        logger.debug( "Building profile manager for model: " + model.getId() + " with pom file: "
//                      + pomFile );
        ProfileManager profileManager = buildProfileManager( model, pomFile, useProfilesXml, activationContext );

        return applyActivatedProfiles( model, pomFile, profileManager );
    }

    public List applyActivatedExternalProfiles( Model model, File projectDir, ProfileManager externalProfileManager )
        throws ProjectBuildingException
    {
        if ( externalProfileManager == null )
        {
            return Collections.EMPTY_LIST;
        }

//        logger.debug( "Building profile manager for model: " + model.getId()
//                      + " with external profile manager including profiles: "
//                      + externalProfileManager.getProfilesById() );
        return applyActivatedProfiles( model, projectDir, externalProfileManager );
    }

    private List applyActivatedProfiles( Model model, File pomFile, ProfileManager profileManager )
        throws ProjectBuildingException
    {
        List activeProfiles;

        if ( profileManager != null )
        {
            try
            {
                activeProfiles = profileManager.getActiveProfiles( model );
            }
            catch ( ProfileActivationException e )
            {
                String groupId = model.getGroupId();
                if ( groupId == null )
                {
                    groupId = "unknown";
                }

                String artifactId = model.getArtifactId();
                if ( artifactId == null )
                {
                    artifactId = "unknown";
                }

                String projectId = ArtifactUtils.versionlessKey( groupId, artifactId );

                throw new ProjectBuildingException( projectId, e.getMessage(), pomFile, e );
            }

            for ( Iterator it = activeProfiles.iterator(); it.hasNext(); )
            {
                Profile profile = (Profile) it.next();

                profileInjector.inject( profile, model );
            }
        }
        else
        {
            activeProfiles = Collections.EMPTY_LIST;
        }

        return activeProfiles;
    }

    private ProfileManager buildProfileManager( Model model, File pomFile, boolean useProfilesXml,
                                                ProfileActivationContext profileActivationContext )
        throws ProjectBuildingException
    {
        ProfileManager profileManager = new DefaultProfileManager( container, profileActivationContext );

        profileManager.addProfiles( model.getProfiles() );

        if ( useProfilesXml && ( pomFile != null ) )
        {
            loadExternalProjectProfiles( profileManager, model, pomFile );
        }

        return profileManager;
    }

    public LinkedHashSet getArtifactRepositoriesFromActiveProfiles( Model model, File pomFile,
                                                                    ProfileManager profileManager )
        throws ProjectBuildingException
    {

        if ( profileManager == null )
        {
            return new LinkedHashSet();
        }

        List activeExternalProfiles;
        {
            try
            {
                activeExternalProfiles = profileManager.getActiveProfiles( model );
            }
            catch ( ProfileActivationException e )
            {
                throw new ProjectBuildingException( model.getId(),
                                                    "Failed to compute active profiles for repository aggregation.",
                                                    pomFile, e );
            }

            LinkedHashSet remoteRepositories = new LinkedHashSet();

            for ( Iterator i = activeExternalProfiles.iterator(); i.hasNext(); )
            {
                Profile externalProfile = (Profile) i.next();
                if ( externalProfile.getRepositories() != null )
                {
                    for ( Iterator repoIterator = externalProfile.getRepositories().iterator();
                          repoIterator.hasNext(); )
                    {
                        Repository mavenRepo = (Repository) repoIterator.next();

                        ArtifactRepository artifactRepo;
                        try
                        {
                            artifactRepo = mavenTools.buildArtifactRepository( mavenRepo );
                        }
                        catch ( InvalidRepositoryException e )
                        {
                            throw new ProjectBuildingException( model.getId(), e.getMessage(), e );
                        }

                        remoteRepositories.add( artifactRepo );
                    }
                }
            }

            return remoteRepositories;
        }
    }

    public LinkedHashSet getArtifactRepositoriesFromActiveProfiles( Model model, File pomFile, boolean useProfilesXml,
                                                                    ProfileActivationContext activationContext )
        throws ProjectBuildingException
    {
        ProfileManager profileManager = buildProfileManager( model, pomFile, useProfilesXml, activationContext );

        return getArtifactRepositoriesFromActiveProfiles( model, pomFile, profileManager );
    }

    private void loadExternalProjectProfiles( ProfileManager profileManager, Model model, File pomFile )
        throws ProjectBuildingException
    {
        if ( pomFile != null )
        {
            File projectDir = pomFile.getParentFile();
            try
            {
                ProfilesRoot root = profilesBuilder.buildProfiles( projectDir );

                if ( root != null )
                {
                    List active = root.getActiveProfiles();

                    if ( ( active != null ) && !active.isEmpty() )
                    {
                        ProfileActivationContext ctx = profileManager.getProfileActivationContext();
                        ctx.setExplicitlyActiveProfileIds( root.getActiveProfiles() );
                    }

                    for ( Iterator it = root.getProfiles().iterator(); it.hasNext(); )
                    {
                        org.apache.maven.profiles.Profile rawProfile = (org.apache.maven.profiles.Profile) it.next();

                        Profile converted = ProfilesConversionUtils.convertFromProfileXmlProfile( rawProfile );

                        profileManager.addProfile( converted );
                    }
                }
            }
            catch ( IOException e )
            {
                throw new ProjectBuildingException( model.getId(),
                                                    "Cannot read profiles.xml resource from directory: " + projectDir,
                                                    pomFile, e );
            }
            catch ( XmlPullParserException e )
            {
                throw new ProjectBuildingException( model.getId(),
                                                    "Cannot parse profiles.xml resource from directory: " + projectDir,
                                                    pomFile, e );
            }
        }
    }

    public void contextualize( Context context )
        throws ContextException
    {
        container = (PlexusContainer) context.get( PlexusConstants.PLEXUS_KEY );
    }

    public void enableLogging( Logger logger )
    {
        this.logger = logger;
    }

}
