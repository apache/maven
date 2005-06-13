package org.apache.maven.settings;

import org.codehaus.plexus.util.StringUtils;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

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

public final class SettingsUtils
{

    private SettingsUtils()
    {
    }

    public static void merge( Settings dominant, Settings recessive )
    {
        if ( dominant == null || recessive == null )
        {
            return;
        }

        List dominantActiveProfiles = dominant.getActiveProfiles();
        List recessiveActiveProfiles = recessive.getActiveProfiles();

        for ( Iterator it = recessiveActiveProfiles.iterator(); it.hasNext(); )
        {
            String profileId = (String) it.next();

            if ( !dominantActiveProfiles.contains( profileId ) )
            {
                dominantActiveProfiles.add( profileId );
            }
        }

        if ( StringUtils.isEmpty( dominant.getLocalRepository() ) )
        {
            dominant.setLocalRepository( recessive.getLocalRepository() );
        }

        List mergedMirrors = new ArrayList( dominant.getMirrors() );

        List recessiveMirrors = recessive.getMirrors();

        Map dominantMirrors = dominant.getMirrorsAsMap();

        for ( Iterator it = recessiveMirrors.iterator(); it.hasNext(); )
        {
            Mirror recessiveMirror = (Mirror) it.next();

            Mirror dominantMirror = (Mirror) dominantMirrors.get( recessiveMirror.getId() );

            if ( dominantMirror == null )
            {
                mergedMirrors.add( recessiveMirror );
            }
        }

        dominant.setMirrors( mergedMirrors );

        List mergedServers = new ArrayList( dominant.getServers() );

        List recessiveServers = recessive.getServers();

        Map dominantServers = dominant.getServersAsMap();

        for ( Iterator it = recessiveServers.iterator(); it.hasNext(); )
        {
            Server recessiveServer = (Server) it.next();

            if ( !dominantServers.containsKey( recessiveServer.getId() ) )
            {
                mergedServers.add( recessiveServer );
            }
        }

        dominant.setServers( mergedServers );

        List mergedProxies = new ArrayList( dominant.getProxies() );

        List recessiveProxies = recessive.getProxies();

        Map dominantProxies = dominant.getProxiesAsMap();

        for ( Iterator it = recessiveProxies.iterator(); it.hasNext(); )
        {
            Proxy recessiveProxy = (Proxy) it.next();

            if ( !dominantProxies.containsKey( recessiveProxy ) )
            {
                mergedProxies.add( recessiveProxy );
            }
        }

        dominant.setProxies( mergedProxies );

        List mergedProfiles = new ArrayList( dominant.getProfiles() );

        List recessiveProfiles = recessive.getProfiles();

        Map dominantProfiles = dominant.getProfilesAsMap();

        for ( Iterator it = recessiveProfiles.iterator(); it.hasNext(); )
        {
            Profile recessiveProfile = (Profile) it.next();

            if ( !dominantProfiles.containsKey( recessiveProfile.getId() ) )
            {
                mergedProfiles.add( recessiveProfile );
            }
        }

        dominant.setProfiles( mergedProfiles );

    }

    public static org.apache.maven.model.Profile convertFromSettingsProfile( Profile settingsProfile )
    {
        org.apache.maven.model.Profile profile = new org.apache.maven.model.Profile();

        profile.setId( settingsProfile.getId() );

        profile.setSource( "settings.xml" );

        org.apache.maven.settings.Activation settingsActivation = settingsProfile.getActivation();

        if ( settingsActivation != null )
        {
            org.apache.maven.model.Activation activation = new org.apache.maven.model.Activation();

            activation.setJdk( settingsActivation.getJdk() );

            org.apache.maven.settings.ActivationProperty settingsProp = settingsActivation.getProperty();

            if ( settingsProp != null )
            {
                org.apache.maven.model.ActivationProperty prop = new org.apache.maven.model.ActivationProperty();

                prop.setName( settingsProp.getName() );
                prop.setValue( settingsProp.getValue() );

                activation.setProperty( prop );
            }
        }

        profile.setProperties( settingsProfile.getProperties() );

        List repos = settingsProfile.getRepositories();
        if ( repos != null )
        {
            for ( Iterator it = repos.iterator(); it.hasNext(); )
            {
                profile.addRepository( convertFromSettingsRepository( (Repository) it.next() ) );
            }
        }

        List pluginRepos = settingsProfile.getPluginRepositories();
        if ( pluginRepos != null )
        {
            for ( Iterator it = pluginRepos.iterator(); it.hasNext(); )
            {
                profile.addPluginRepository( convertFromSettingsRepository( (Repository) it.next() ) );
            }
        }

        return profile;
    }

    private static org.apache.maven.model.Repository convertFromSettingsRepository( Repository settingsRepo )
    {
        org.apache.maven.model.Repository repo = new org.apache.maven.model.Repository();

        repo.setId( settingsRepo.getId() );
        repo.setLayout( settingsRepo.getLayout() );
        repo.setName( settingsRepo.getName() );
        repo.setSnapshotPolicy( settingsRepo.getSnapshotPolicy() );
        repo.setUrl( settingsRepo.getUrl() );

        return repo;
    }

}
