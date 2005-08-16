package org.apache.maven.profiles;

import org.apache.maven.model.Profile;
import org.apache.maven.profiles.activation.ProfileActivationException;
import org.apache.maven.profiles.activation.ProfileActivator;
import org.codehaus.plexus.PlexusContainer;
import org.codehaus.plexus.component.repository.exception.ComponentLifecycleException;
import org.codehaus.plexus.component.repository.exception.ComponentLookupException;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.Map.Entry;

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

public class DefaultProfileManager implements ProfileManager
{
    private PlexusContainer container;

    private Set activatedIds = new HashSet();
    private Set deactivatedIds = new HashSet();
    
    private Map profilesById = new HashMap();
    
    public DefaultProfileManager( PlexusContainer container )
    {
        this.container = container;
    }
    
    public DefaultProfileManager( ProfileManager globals, PlexusContainer container )
    {
        this.container = container;
        
        this.activatedIds.addAll( globals.getActivatedIds() );
        this.deactivatedIds.addAll( globals.getDeactivatedIds() );
        this.profilesById.putAll( globals.getProfilesById() );
    }
    
    public Set getActivatedIds()
    {
        return activatedIds;
    }
    
    public Set getDeactivatedIds()
    {
        return deactivatedIds;
    }
    
    public Map getProfilesById()
    {
        return profilesById;
    }
    
    /* (non-Javadoc)
     * @see org.apache.maven.profiles.ProfileManager#addProfile(org.apache.maven.model.Profile)
     */
    public void addProfile( Profile profile )
    {
        String profileId = profile.getId();
        
        Profile existing = (Profile) profilesById.get( profileId );
        if ( existing != null )
        {
            container.getLogger().warn(
                                        "Overriding profile: \'" + profileId + "\' (source: " + existing.getSource()
                                            + ") with new instance from source: " + profile.getSource() );
        }
        
        profilesById.put( profile.getId(), profile );
    }
    
    /* (non-Javadoc)
     * @see org.apache.maven.profiles.ProfileManager#explicitlyActivate(java.lang.String)
     */
    public void explicitlyActivate( String profileId )
    {
        container.getLogger().debug( "Profile with id: \'" + profileId + "\' has been explicitly activated." );
        
        activatedIds.add( profileId );
    }
    
    /* (non-Javadoc)
     * @see org.apache.maven.profiles.ProfileManager#explicitlyActivate(java.util.List)
     */
    public void explicitlyActivate( List profileIds )
    {
        for ( Iterator it = profileIds.iterator(); it.hasNext(); )
        {
            String profileId = (String) it.next();
            
            explicitlyActivate( profileId );
        }
    }
    
    /* (non-Javadoc)
     * @see org.apache.maven.profiles.ProfileManager#explicitlyDeactivate(java.lang.String)
     */
    public void explicitlyDeactivate( String profileId )
    {
        container.getLogger().debug( "Profile with id: \'" + profileId + "\' has been explicitly deactivated." );
        
        deactivatedIds.add( profileId );
    }
    
    /* (non-Javadoc)
     * @see org.apache.maven.profiles.ProfileManager#explicitlyDeactivate(java.util.List)
     */
    public void explicitlyDeactivate( List profileIds )
    {
        for ( Iterator it = profileIds.iterator(); it.hasNext(); )
        {
            String profileId = (String) it.next();
            
            explicitlyDeactivate( profileId );
        }
    }
    
    /* (non-Javadoc)
     * @see org.apache.maven.profiles.ProfileManager#getActiveProfiles()
     */
    public List getActiveProfiles() throws ProfileActivationException
    {
        List active = new ArrayList( profilesById.size() );
        
        for ( Iterator it = profilesById.entrySet().iterator(); it.hasNext(); )
        {
            Map.Entry entry = (Entry) it.next();
            
            String profileId = (String) entry.getKey();
            Profile profile = (Profile) entry.getValue();
            
            if ( activatedIds.contains( profileId ) )
            {
                active.add( profile );
            }
            else if ( !deactivatedIds.contains( profileId ) && isActive( profile ) )
            {
                active.add( profile );
            }
        }
        
        return active;
    }
    
    private boolean isActive( Profile profile )
        throws ProfileActivationException
    {
        List activators = null;
        try
        {
            activators = container.lookupList( ProfileActivator.ROLE );

            for ( Iterator activatorIterator = activators.iterator(); activatorIterator.hasNext(); )
            {
                ProfileActivator activator = (ProfileActivator) activatorIterator.next();

                if ( activator.canDetermineActivation( profile ) )
                {
                    return activator.isActive( profile );
                }
            }
            
            return false;
        }
        catch ( ComponentLookupException e )
        {
            throw new ProfileActivationException( "Cannot retrieve list of profile activators.", e );
        }
        finally
        {
            try
            {
                container.releaseAll( activators );
            }
            catch ( ComponentLifecycleException e )
            {
                container.getLogger().debug( "Error releasing profile activators - ignoring.", e );
            }
        }
    }

    /* (non-Javadoc)
     * @see org.apache.maven.profiles.ProfileManager#addProfiles(java.util.List)
     */
    public void addProfiles( List profiles )
    {
        for ( Iterator it = profiles.iterator(); it.hasNext(); )
        {
            Profile profile = (Profile) it.next();
            
            addProfile( profile );
        }
    }
    
}
