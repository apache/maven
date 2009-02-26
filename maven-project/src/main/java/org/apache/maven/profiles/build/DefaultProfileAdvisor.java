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

import org.apache.maven.artifact.ArtifactUtils;
import org.apache.maven.model.Model;
import org.apache.maven.model.Profile;
import org.apache.maven.profiles.DefaultProfileManager;
import org.apache.maven.profiles.ProfileManager;
import org.apache.maven.profiles.activation.ProfileActivationContext;
import org.apache.maven.profiles.activation.ProfileActivationException;
import org.apache.maven.profiles.injection.ProfileInjector;
import org.apache.maven.project.ProjectBuildingException;
import org.codehaus.plexus.PlexusConstants;
import org.codehaus.plexus.PlexusContainer;
import org.codehaus.plexus.component.annotations.Component;
import org.codehaus.plexus.component.annotations.Requirement;
import org.codehaus.plexus.context.Context;
import org.codehaus.plexus.context.ContextException;
import org.codehaus.plexus.logging.LogEnabled;
import org.codehaus.plexus.logging.Logger;
import org.codehaus.plexus.personality.plexus.lifecycle.phase.Contextualizable;

import java.util.Collections;
import java.util.Iterator;
import java.util.List;

@Component(role = ProfileAdvisor.class)
public class DefaultProfileAdvisor
    implements ProfileAdvisor, LogEnabled, Contextualizable
{

    @Requirement
    private ProfileInjector profileInjector;

    @Requirement
    private PlexusContainer container;

    private Logger logger;

    public List applyActivatedProfiles(Model model,
                                       ProfileActivationContext activationContext)
        throws ProjectBuildingException
    {
        ProfileManager profileManager = buildProfileManager( model, activationContext );

        return applyActivatedProfiles( model, profileManager );
    }

    public List applyActivatedExternalProfiles(Model model, ProfileManager externalProfileManager)
        throws ProjectBuildingException
    {
        if ( externalProfileManager == null )
        {
            return Collections.EMPTY_LIST;
        }
        
        return applyActivatedProfiles( model, externalProfileManager );
    }

    private List applyActivatedProfiles( Model model, ProfileManager profileManager )
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

                throw new ProjectBuildingException(projectId, e.getMessage());
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

    private ProfileManager buildProfileManager(Model model,
                                               ProfileActivationContext profileActivationContext)
        throws ProjectBuildingException
    {
        ProfileManager profileManager = new DefaultProfileManager( container, profileActivationContext );

        profileManager.addProfiles( model.getProfiles() );

        return profileManager;
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
