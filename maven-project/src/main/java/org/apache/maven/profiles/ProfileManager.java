package org.apache.maven.profiles;

import java.util.Properties;
import org.apache.maven.model.Profile;
import org.apache.maven.profiles.activation.ProfileActivationException;
import org.apache.maven.settings.Settings;

import java.util.List;
import java.util.Map;

public interface ProfileManager
{

    void addProfile( Profile profile );

    void explicitlyActivate( String profileId );

    void explicitlyActivate( List profileIds );

    void explicitlyDeactivate( String profileId );

    void explicitlyDeactivate( List profileIds );

    void activateAsDefault( String profileId );

    List getActiveProfiles()
        throws ProfileActivationException;

    void addProfiles( List profiles );

    Map getProfilesById();

    List getExplicitlyActivatedIds();

    List getExplicitlyDeactivatedIds();

    List getIdsActivatedByDefault();

    void loadSettingsProfiles( Settings settings );
    
}