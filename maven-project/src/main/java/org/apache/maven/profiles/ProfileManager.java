package org.apache.maven.profiles;

import org.apache.maven.model.Profile;
import org.apache.maven.profiles.activation.ProfileActivationException;

import java.util.List;
import java.util.Map;
import java.util.Set;

public interface ProfileManager
{

    void addProfile( Profile profile );

    void explicitlyActivate( String profileId );

    void explicitlyActivate( List profileIds );

    void explicitlyDeactivate( String profileId );

    void explicitlyDeactivate( List profileIds );

    List getActiveProfiles()
        throws ProfileActivationException;

    void addProfiles( List profiles );

    public Set getActivatedIds();
    
    public Set getDeactivatedIds();
    
    public Map getProfilesById();
    
}