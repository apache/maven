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
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */

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
