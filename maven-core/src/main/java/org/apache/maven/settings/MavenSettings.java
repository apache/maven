package org.apache.maven.settings;

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

import java.util.Iterator;
import java.util.List;

/**
 * @author jdcasey
 * @version $Id$
 */
public class MavenSettings
{
    private static final String DEFAULT_LOCAL_REPOSITORY = "/.m2/repository";

    private final Settings settings;

    public MavenSettings()
    {
        settings = new Settings();

        Profile profile = new Profile();
        profile.setActive( true );

        String userHome = System.getProperty( "user.home" );
        profile.setLocalRepository( userHome + DEFAULT_LOCAL_REPOSITORY );

        settings.addProfile( profile );
    }

    public MavenSettings( Settings settings )
    {
        this.settings = settings;
    }

    public Server getServer( String serverId )
    {
        Server match = null;

        List servers = settings.getServers();
        if ( servers != null && serverId != null )
        {
            for ( Iterator it = servers.iterator(); it.hasNext(); )
            {
                Server server = (Server) it.next();
                if ( serverId.equals( server.getId() ) )
                {
                    match = server;
                    break;
                }
            }
        }

        return match;
    }

    public Profile getActiveProfile()
    {
        Profile active = null;

        List profiles = settings.getProfiles();
        if ( profiles != null && !profiles.isEmpty() )
        {
            if ( profiles.size() > 1 )
            {
                for ( Iterator it = profiles.iterator(); it.hasNext(); )
                {
                    Profile profile = (Profile) it.next();
                    if ( profile.isActive() )
                    {
                        active = profile;
                        break;
                    }
                }
            }
            else
            {
                // If we only have one profile, use it as the active one.
                active = (Profile) profiles.get( 0 );
            }
        }

        return active;
    }

    public Proxy getActiveProxy()
    {
        Proxy active = null;

        List proxies = settings.getProxies();
        if ( proxies != null && !proxies.isEmpty() )
        {
            if ( proxies.size() > 1 )
            {
                for ( Iterator it = proxies.iterator(); it.hasNext(); )
                {
                    Proxy proxy = (Proxy) it.next();
                    if ( proxy.isActive() )
                    {
                        active = proxy;
                        break;
                    }
                }
            }
            else
            {
                // If we only have one profile, use it as the active one.
                active = (Proxy) proxies.get( 0 );
            }
        }

        return active;
    }
}
