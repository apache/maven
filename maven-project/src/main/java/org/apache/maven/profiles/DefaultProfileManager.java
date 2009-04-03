package org.apache.maven.profiles;

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

import org.apache.maven.model.Activation;
import org.apache.maven.model.Model;
import org.apache.maven.model.Profile;
import org.apache.maven.model.Parent;
import org.apache.maven.profiles.ProfileActivationContext;
import org.apache.maven.profiles.ProfileActivationException;
import org.apache.maven.profiles.ProfileManager;
import org.apache.maven.profiles.matchers.DefaultMatcher;
import org.apache.maven.profiles.matchers.ProfileMatcher;
import org.apache.maven.profiles.matchers.PropertyMatcher;
import org.apache.maven.shared.model.InterpolatorProperty;
import org.apache.maven.project.ProjectBuilderConfiguration;
import org.apache.maven.project.builder.PomInterpolatorTag;
import org.codehaus.plexus.PlexusContainer;
import org.codehaus.plexus.MutablePlexusContainer;

import java.util.*;
import java.util.Map.Entry;

public class DefaultProfileManager
    implements ProfileManager
{
    private MutablePlexusContainer container;

    private Map<String, Profile> profilesById = new LinkedHashMap<String, Profile>();

    private ProfileActivationContext profileActivationContext;
    
    private static final ProfileMatcher defaultMatcher = new DefaultMatcher();

    private static final List<ProfileMatcher> matchers =
        Collections.unmodifiableList( Arrays.asList( new DefaultMatcher(), new PropertyMatcher() ) );    

    /**
     * the properties passed to the profile manager are the props that
     * are passed to maven, possibly containing profile activator properties
     */
    public DefaultProfileManager( PlexusContainer container, ProfileActivationContext profileActivationContext )
    {
        this.container = (MutablePlexusContainer) container;
        if ( profileActivationContext == null )
        {
            this.profileActivationContext = createDefaultActivationContext();
        }
        else
        {
            this.profileActivationContext = profileActivationContext;
        }
    }

    // TODO: Remove this, if possible. It uses system properties, which are not safe for IDE and other embedded environments.
    /**
     * @deprecated Using this is dangerous when extensions or non-global system properties are in play.
     */
    public DefaultProfileManager( PlexusContainer container )
    {
        this.container = (MutablePlexusContainer) container;

        profileActivationContext = createDefaultActivationContext();
    }

    private ProfileActivationContext createDefaultActivationContext()
    {

        return new ProfileActivationContext(System.getProperties(), false );
    }

    public ProfileActivationContext getProfileActivationContext()
    {
        return profileActivationContext;
    }

    public void setProfileActivationContext( ProfileActivationContext profileActivationContext )
    {
        this.profileActivationContext = profileActivationContext;
    }

    public Map<String, Profile> getProfilesById()
    {
        return profilesById;
    }

    /* (non-Javadoc)
    * @see org.apache.maven.project.ProfileManager#addProfile(org.apache.maven.model.Profile)
    */
    public void addProfile( Profile profile )
    {
        String profileId = profile.getId();

        Profile existing = (Profile) profilesById.get( profileId );
        if ( existing != null )
        {
            container.getLogger().warn( "Overriding profile: \'" + profileId + "\' (source: " + existing.getSource() +
                ") with new instance from source: " + profile.getSource() );
        }

        profilesById.put( profile.getId(), profile );

        Activation activation = profile.getActivation();

        if ( ( activation != null ) && activation.isActiveByDefault() )
        {
            activateAsDefault( profileId );
        }
    }

	public List<Profile> getActiveProfiles() throws ProfileActivationException {
		return getActiveProfiles(null);
	}
	
    public List<Profile> getActiveProfiles( Model model )
        throws ProfileActivationException
    {
        List<Profile> activeFromPom = new ArrayList<Profile>();
        List<Profile> activeExternal = new ArrayList<Profile>();

        for ( Iterator it = profilesById.entrySet().iterator(); it.hasNext(); )
        {
            Map.Entry entry = (Entry) it.next();

            String profileId = (String) entry.getKey();
            Profile profile = (Profile) entry.getValue();

            boolean shouldAdd = false;
            if ( profileActivationContext.isExplicitlyActive( profileId ) )
            {
                shouldAdd = true;
            }
            else if ( isActive( profile, profileActivationContext ) )
            {
                shouldAdd = true;
            }

            if ( !profileActivationContext.isExplicitlyInactive( profileId ) && shouldAdd )
            {
                if ( "pom".equals( profile.getSource() ) )
                {
                    activeFromPom.add( profile );
                }
                else
                {
                    activeExternal.add( profile );
                }
            }
        }

        if ( activeFromPom.isEmpty() )
        {
            List<String> defaultIds = profileActivationContext.getActiveByDefaultProfileIds();

            List<String> deactivatedIds = profileActivationContext.getExplicitlyInactiveProfileIds();

            for ( String profileId : defaultIds )
            {
                // If this profile was excluded, don't add it back in
                // Fixes MNG-3545
                if ( deactivatedIds.contains( profileId ) )
                {
                    continue;
                }
                Profile profile = (Profile) profilesById.get( profileId );

                if ( profile != null )
                {
                    activeFromPom.add( profile );
                }
            }
        }

        List<Profile> allActive = new ArrayList<Profile>( activeFromPom.size() + activeExternal.size() );
       // System.out.println("Active From POM: " + activeFromPom.size() + ": EXTERNAL:" + activeExternal.size());
        allActive.addAll( activeExternal );
        allActive.addAll( activeFromPom );
     
        List<Profile> defaults = getDefaultProfiles(allActive);
        if(defaults.size() < allActive.size())
        {
            allActive.removeAll( defaults );
        }
        return allActive;
    }
    
    public static List<Profile> getActiveProfilesFrom(ProjectBuilderConfiguration config, Model model, PlexusContainer container)
		throws ProfileActivationException
	{
	    List<Profile> projectProfiles = new ArrayList<Profile>();
	    ProfileManager externalProfileManager = config.getGlobalProfileManager();
	    
	    ProfileActivationContext profileActivationContext = (externalProfileManager == null) ? new ProfileActivationContext( config.getExecutionProperties(), false ):
	        externalProfileManager.getProfileActivationContext();
	 
	    if(externalProfileManager != null)
	    {           
	    	projectProfiles.addAll( externalProfileManager.getActiveProfiles() );    
	    }
	
	    ProfileManager profileManager = new DefaultProfileManager( container, profileActivationContext );
	    profileManager.addProfiles( model.getProfiles() );
	    projectProfiles.addAll( profileManager.getActiveProfiles() ); 
	    return projectProfiles;
	}   
 
    public static Collection<Profile> getActiveProfiles(List<Profile> profiles, ProfileManagerInfo profileContextInfo)
    {
        List<InterpolatorProperty> properties = profileContextInfo.getInterpolatorProperties();
        Collection<String> activeProfileIds = profileContextInfo.getActiveProfileIds();
        Collection<String> inactiveProfileIds = profileContextInfo.getInactiveProfileIds();
        
        List<Profile> matchedProfiles = new ArrayList<Profile>();
        List<Profile> defaultProfiles = new ArrayList<Profile>();
        for ( Profile profile : profiles )
        {
            String profileId = profile.getId();

            if ( !inactiveProfileIds.contains( profileId ) )
            {
                if ( activeProfileIds.contains( profileId ) )
                {
                    matchedProfiles.add( profile );
                }
                else if ( defaultMatcher.isMatch( profile, properties ) )
                {
                    defaultProfiles.add( profile );
                }
                else
                {
                    for ( ProfileMatcher matcher : matchers )
                    {
                        if ( matcher.isMatch( profile, properties ) )
                        {
                            matchedProfiles.add( profile );
                            break;
                        }
                    }
                }
            }
        }

        if ( matchedProfiles.isEmpty() )
        {
            matchedProfiles = defaultProfiles;
        }

        return matchedProfiles;
    }    

    /* (non-Javadoc)
     * @see org.apache.maven.project.ProfileManager#addProfiles(java.util.List)
     */
    public void addProfiles( List<Profile> profiles )
    {
        for ( Iterator it = profiles.iterator(); it.hasNext(); )
        {
            Profile profile = (Profile) it.next();

            addProfile( profile );
        }
    }   
    
    private static List<Profile> getDefaultProfiles(List<Profile> profiles)
    {
        List<Profile> defaults = new ArrayList<Profile>();
        for(Profile p : profiles)
        {
            if( (p.getActivation() != null && p.getActivation().isActiveByDefault()) || p.getActivation() == null )
            {
                defaults.add( p );
            }
        }
        return defaults;
    }

    private boolean isActive( Profile profile, ProfileActivationContext context )
        throws ProfileActivationException
    {
        List<InterpolatorProperty> interpolatorProperties = new ArrayList<InterpolatorProperty>();
        if(context.getExecutionProperties() != null)
        {
            interpolatorProperties.addAll(InterpolatorProperty.toInterpolatorProperties(
                                                                                        context.getExecutionProperties(),
                                                                                        PomInterpolatorTag.EXECUTION_PROPERTIES.name()));              
        }
     
        for(ProfileMatcher matcher : matchers)
        {
            if(matcher.isMatch(profile, interpolatorProperties))
            {
                return true;
            }
        }
        return false;
    }

    private void activateAsDefault( String profileId )
    {
        List defaultIds = profileActivationContext.getActiveByDefaultProfileIds();

        if ( !defaultIds.contains( profileId ) )
        {
            profileActivationContext.setActiveByDefault( profileId );
        }
    }
}
