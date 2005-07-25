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

import org.codehaus.plexus.util.StringUtils;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

public final class SettingsUtils
{
    private SettingsUtils()
    {
        // don't allow construction.
    }

    public static void merge( Settings dominant, Settings recessive, String recessiveSourceLevel )
    {
        if ( dominant == null || recessive == null )
        {
            return;
        }

        recessive.setSourceLevel( recessiveSourceLevel );

        List dominantActiveProfiles = dominant.getActiveProfiles();
        List recessiveActiveProfiles = recessive.getActiveProfiles();

        if ( recessiveActiveProfiles != null )
        {
            if ( dominantActiveProfiles == null )
            {
                dominantActiveProfiles = new ArrayList();
                dominant.setActiveProfiles( dominantActiveProfiles );
            }

            for ( Iterator it = recessiveActiveProfiles.iterator(); it.hasNext(); )
            {
                String profileId = (String) it.next();

                if ( !dominantActiveProfiles.contains( profileId ) )
                {
                    dominantActiveProfiles.add( profileId );

                    dominant.getRuntimeInfo().setActiveProfileSourceLevel( profileId, recessiveSourceLevel );
                }
            }
        }

        List dominantPluginGroupIds = dominant.getPluginGroups();
        List recessivePluginGroupIds = recessive.getPluginGroups();

        if ( recessivePluginGroupIds != null )
        {
            if ( dominantPluginGroupIds == null )
            {
                dominantPluginGroupIds = new ArrayList();
                dominant.setPluginGroups( dominantPluginGroupIds );
            }

            for ( Iterator it = recessivePluginGroupIds.iterator(); it.hasNext(); )
            {
                String pluginGroupId = (String) it.next();

                if ( !dominantPluginGroupIds.contains( pluginGroupId ) )
                {
                    dominantPluginGroupIds.add( pluginGroupId );

                    dominant.getRuntimeInfo().setPluginGroupIdSourceLevel( pluginGroupId, recessiveSourceLevel );
                }
            }
        }

        if ( StringUtils.isEmpty( dominant.getLocalRepository() ) )
        {
            dominant.setLocalRepository( recessive.getLocalRepository() );

            dominant.getRuntimeInfo().setLocalRepositorySourceLevel( recessiveSourceLevel );
        }

        shallowMergeById( dominant.getMirrors(), recessive.getMirrors(), recessiveSourceLevel );
        shallowMergeById( dominant.getServers(), recessive.getServers(), recessiveSourceLevel );
        shallowMergeById( dominant.getProxies(), recessive.getProxies(), recessiveSourceLevel );
        shallowMergeById( dominant.getProfiles(), recessive.getProfiles(), recessiveSourceLevel );

    }

    private static void shallowMergeById( List dominant, List recessive, String recessiveSourceLevel )
    {
        Map dominantById = mapById( dominant );

        for ( Iterator it = recessive.iterator(); it.hasNext(); )
        {
            IdentifiableBase identifiable = (IdentifiableBase) it.next();

            if ( !dominantById.containsKey( identifiable.getId() ) )
            {
                identifiable.setSourceLevel( recessiveSourceLevel );

                dominant.add( identifiable );
            }
        }
    }

    private static Map mapById( List identifiables )
    {
        Map byId = new HashMap();

        for ( Iterator it = identifiables.iterator(); it.hasNext(); )
        {
            IdentifiableBase identifiable = (IdentifiableBase) it.next();

            byId.put( identifiable.getId(), identifiable );
        }

        return byId;
    }

    public static org.apache.maven.model.Profile convertFromSettingsProfile( Profile settingsProfile )
    {
        org.apache.maven.model.Profile profile = new org.apache.maven.model.Profile();

        profile.setId( settingsProfile.getId() );

        profile.setSource( "settings.xml" );

        Activation settingsActivation = settingsProfile.getActivation();

        if ( settingsActivation != null )
        {
            org.apache.maven.model.Activation activation = new org.apache.maven.model.Activation();

            activation.setJdk( settingsActivation.getJdk() );

            ActivationProperty settingsProp = settingsActivation.getProperty();

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
        repo.setChecksumPolicy( settingsRepo.getChecksumPolicy() );
        repo.setUrl( settingsRepo.getUrl() );

        if ( settingsRepo.getSnapshots() != null )
        {
            repo.setSnapshots( convertRepositoryPolicy( settingsRepo.getSnapshots() ) );
        }
        if ( settingsRepo.getReleases() != null )
        {
            repo.setReleases( convertRepositoryPolicy( settingsRepo.getReleases() ) );
        }

        return repo;
    }

    private static org.apache.maven.model.RepositoryPolicy convertRepositoryPolicy( RepositoryPolicy settingsPolicy )
    {
        org.apache.maven.model.RepositoryPolicy policy = new org.apache.maven.model.RepositoryPolicy();
        policy.setEnabled( settingsPolicy.isEnabled() );
        policy.setUpdatePolicy( settingsPolicy.getUpdatePolicy() );
        policy.setChecksumPolicy( settingsPolicy.getChecksumPolicy() );
        return policy;
    }

}
