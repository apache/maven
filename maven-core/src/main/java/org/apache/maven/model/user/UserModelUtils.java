package org.apache.maven.model.user;

/* ====================================================================
 *   Copyright 2001-2004 The Apache Software Foundation.
 *
 *   Licensed under the Apache License, Version 2.0 (the "License");
 *   you may not use this file except in compliance with the License.
 *   You may obtain a copy of the License at
 *
 *       http://www.apache.org/licenses/LICENSE-2.0
 *
 *   Unless required by applicable law or agreed to in writing, software
 *   distributed under the License is distributed on an "AS IS" BASIS,
 *   WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *   See the License for the specific language governing permissions and
 *   limitations under the License.
 * ====================================================================
 */

import java.util.Iterator;
import java.util.List;

/**
 * @author jdcasey
 */
public final class UserModelUtils
{

    private static final String ACTIVE_MAVEN_PROFILE_ID_ENVAR = "maven.profile";

    private UserModelUtils()
    {
    }

    public static MavenProfile getActiveMavenProfile( UserModel userModel )
    {
        String activeProfileId = System.getProperty( ACTIVE_MAVEN_PROFILE_ID_ENVAR );
        if ( activeProfileId == null || activeProfileId.trim().length() < 1 )
        {
            DefaultProfiles defaults = userModel.getDefaultProfiles();
            if ( defaults != null )
            {
                activeProfileId = defaults.getMavenProfileId();
            }
        }

        MavenProfile activeProfile = null;

        if ( activeProfileId != null && activeProfileId.trim().length() > 0 )
        {
            activeProfile = UserModelUtils.getMavenProfile( userModel, activeProfileId );
        }

        return activeProfile;
    }

    public static ProxyProfile getActiveProxyProfile( UserModel userModel )
    {
        String activeProfileId = System.getProperty( ACTIVE_MAVEN_PROFILE_ID_ENVAR );
        if ( activeProfileId == null || activeProfileId.trim().length() < 1 )
        {
            DefaultProfiles defaults = userModel.getDefaultProfiles();
            if ( defaults != null )
            {
                activeProfileId = defaults.getProxyProfileId();
            }
        }

        ProxyProfile activeProfile = null;

        if ( activeProfileId != null && activeProfileId.trim().length() > 0 )
        {
            activeProfile = UserModelUtils.getProxyProfile( userModel, activeProfileId );
        }

        return activeProfile;
    }

    public static MavenProfile getMavenProfile( UserModel userModel, String mavenProfileId )
    {
        MavenProfile result = null;

        List mavenProfiles = userModel.getMavenProfiles();
        if ( mavenProfiles != null )
        {
            for ( Iterator it = mavenProfiles.iterator(); it.hasNext(); )
            {
                MavenProfile profile = (MavenProfile) it.next();
                if ( mavenProfileId.equals( profile.getId() ) )
                {
                    result = profile;
                    break;
                }
            }
        }

        return result;
    }

    public static ProxyProfile getProxyProfile( UserModel userModel, String proxyProfileId )
    {
        ProxyProfile result = null;

        List proxyProfile = userModel.getProxyProfiles();
        if ( proxyProfile != null )
        {
            for ( Iterator it = proxyProfile.iterator(); it.hasNext(); )
            {
                ProxyProfile profile = (ProxyProfile) it.next();
                if ( proxyProfileId.equals( profile.getId() ) )
                {
                    result = profile;
                    break;
                }
            }
        }

        return result;
    }

    public static ServerProfile getServerProfile( UserModel userModel, String serverProfileId )
    {
        ServerProfile result = null;

        List serverProfiles = userModel.getServerProfiles();
        if ( serverProfiles != null )
        {
            for ( Iterator it = serverProfiles.iterator(); it.hasNext(); )
            {
                ServerProfile profile = (ServerProfile) it.next();
                if ( serverProfileId.equals( profile.getId() ) )
                {
                    result = profile;
                    break;
                }
            }
        }

        return result;
    }

 }