package org.apache.maven.profiles;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.List;

import org.apache.maven.profiles.matchers.DefaultMatcher;
import org.apache.maven.profiles.matchers.ProfileMatcher;
import org.apache.maven.profiles.matchers.PropertyMatcher;
import org.apache.maven.shared.model.InterpolatorProperty;

import org.apache.maven.model.Profile;

public class ProfileContext
{
    private List<InterpolatorProperty> properties;

    private Collection<String> activeProfileIds;

    private Collection<String> inactiveProfileIds;

    private List<Profile> profiles;

    private ProfileMatcher defaultMatcher = new DefaultMatcher();

    private List<ProfileMatcher> matchers =
        Collections.unmodifiableList( Arrays.asList( (ProfileMatcher) new PropertyMatcher() ) );

    public ProfileContext( List<Profile> profiles, Collection<String> activeProfileIds,
                           Collection<String> inactiveProfileIds, List<InterpolatorProperty> properties )
    {
        this.profiles = new ArrayList<Profile>( profiles );
        this.properties = new ArrayList<InterpolatorProperty>( properties );
        this.activeProfileIds = ( activeProfileIds != null ) ? activeProfileIds : new ArrayList<String>();
        this.inactiveProfileIds = ( inactiveProfileIds != null ) ? inactiveProfileIds : new ArrayList<String>();
    }

    public Collection<Profile> getActiveProfiles()
    {
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
}
