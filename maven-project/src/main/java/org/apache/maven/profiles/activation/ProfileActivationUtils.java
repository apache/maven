package org.apache.maven.profiles.activation;

import org.codehaus.plexus.util.StringUtils;

import java.util.ArrayList;
import java.util.List;
import java.util.StringTokenizer;

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

public final class ProfileActivationUtils
{
    public static final String ACTIVE_PROFILE_IDS = "org.apache.maven.ActiveProfiles";

    private static List profileList;

    private ProfileActivationUtils()
    {
    }

    public static boolean profilesWereExplicitlyGiven()
    {
        return StringUtils.isNotEmpty( System.getProperty( ACTIVE_PROFILE_IDS ) );
    }

    public static List getExplicitProfileList()
    {
        if ( !profilesWereExplicitlyGiven() )
        {
            return null;
        }

        if ( profileList == null )
        {
            profileList = new ArrayList();

            StringTokenizer profileTokens = new StringTokenizer( System.getProperty( ACTIVE_PROFILE_IDS ), "," );

            while ( profileTokens.hasMoreTokens() )
            {
                String token = profileTokens.nextToken().trim();

                if ( StringUtils.isNotEmpty( token ) )
                {
                    profileList.add( token );
                }
            }
        }

        return profileList;
    }

}
