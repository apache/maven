package org.apache.maven.settings;

/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *  http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.maven.model.ActivationFile;
import org.codehaus.plexus.util.StringUtils;

/**
 * Several convenience methods to handle settings
 *
 * @author <a href="mailto:vincent.siveton@gmail.com">Vincent Siveton</a>
 */
public final class SettingsUtils
{
    private SettingsUtils()
    {
        // don't allow construction.
    }

    /**
     * @param dominant
     * @param recessive
     * @param recessiveSourceLevel
     */
    public static void merge( Settings dominant, Settings recessive, String recessiveSourceLevel )
    {
        if ( dominant == null || recessive == null )
        {
            return;
        }

        recessive.setSourceLevel( recessiveSourceLevel );

        List<String> dominantActiveProfiles = dominant.getActiveProfiles();
        List<String> recessiveActiveProfiles = recessive.getActiveProfiles();

        if ( recessiveActiveProfiles != null )
        {
            if ( dominantActiveProfiles == null )
            {
                dominantActiveProfiles = new ArrayList<String>();
                dominant.setActiveProfiles( dominantActiveProfiles );
            }

            for ( String profileId : recessiveActiveProfiles )
            {
                if ( !dominantActiveProfiles.contains( profileId ) )
                {
                    dominantActiveProfiles.add( profileId );
                }
            }
        }

        List<String> dominantPluginGroupIds = dominant.getPluginGroups();

        List<String> recessivePluginGroupIds = recessive.getPluginGroups();

        if ( recessivePluginGroupIds != null )
        {
            if ( dominantPluginGroupIds == null )
            {
                dominantPluginGroupIds = new ArrayList<String>();
                dominant.setPluginGroups( dominantPluginGroupIds );
            }

            for ( String pluginGroupId : recessivePluginGroupIds )
            {
                if ( !dominantPluginGroupIds.contains( pluginGroupId ) )
                {
                    dominantPluginGroupIds.add( pluginGroupId );
                }
            }
        }

        if ( StringUtils.isEmpty( dominant.getLocalRepository() ) )
        {
            dominant.setLocalRepository( recessive.getLocalRepository() );
        }

        shallowMergeById( dominant.getMirrors(), recessive.getMirrors(), recessiveSourceLevel );
        shallowMergeById( dominant.getServers(), recessive.getServers(), recessiveSourceLevel );
        shallowMergeById( dominant.getProxies(), recessive.getProxies(), recessiveSourceLevel );
        shallowMergeById( dominant.getProfiles(), recessive.getProfiles(), recessiveSourceLevel );

    }

    /**
     * @param dominant
     * @param recessive
     * @param recessiveSourceLevel
     */
    private static <T extends IdentifiableBase> void shallowMergeById( List<T> dominant, List<T> recessive,
                                                                       String recessiveSourceLevel )
    {
        Map<String, T> dominantById = mapById( dominant );

        for ( T identifiable : recessive )
        {
            if ( !dominantById.containsKey( identifiable.getId() ) )
            {
                identifiable.setSourceLevel( recessiveSourceLevel );

                dominant.add( identifiable );
            }
        }
    }

    /**
     * @param identifiables
     * @return a map
     */
    private static <T extends IdentifiableBase> Map<String, T> mapById( List<T> identifiables )
    {
        Map<String, T> byId = new HashMap<String, T>();

        for ( T identifiable : identifiables )
        {
            byId.put( identifiable.getId(), identifiable );
        }

        return byId;
    }

    /**
     * @param settingsProfile
     * @return a profile
     */
    public static org.apache.maven.model.Profile convertFromSettingsProfile( Profile settingsProfile )
    {
        org.apache.maven.model.Profile profile = new org.apache.maven.model.Profile();

        profile.setId( settingsProfile.getId() );

        profile.setSource( "settings.xml" );

        Activation settingsActivation = settingsProfile.getActivation();

        if ( settingsActivation != null )
        {
            org.apache.maven.model.Activation activation = new org.apache.maven.model.Activation();

            activation.setActiveByDefault( settingsActivation.isActiveByDefault() );

            activation.setJdk( settingsActivation.getJdk() );

            ActivationProperty settingsProp = settingsActivation.getProperty();

            if ( settingsProp != null )
            {
                org.apache.maven.model.ActivationProperty prop = new org.apache.maven.model.ActivationProperty();

                prop.setName( settingsProp.getName() );
                prop.setValue( settingsProp.getValue() );

                activation.setProperty( prop );
            }

            ActivationOS settingsOs = settingsActivation.getOs();

            if ( settingsOs != null )
            {
                org.apache.maven.model.ActivationOS os = new org.apache.maven.model.ActivationOS();

                os.setArch( settingsOs.getArch() );
                os.setFamily( settingsOs.getFamily() );
                os.setName( settingsOs.getName() );
                os.setVersion( settingsOs.getVersion() );

                activation.setOs( os );
            }

            org.apache.maven.settings.ActivationFile settingsFile = settingsActivation.getFile();

            if ( settingsFile != null )
            {
                ActivationFile file = new ActivationFile();

                file.setExists( settingsFile.getExists() );
                file.setMissing( settingsFile.getMissing() );

                activation.setFile( file );
            }

            profile.setActivation( activation );
        }

        profile.setProperties( settingsProfile.getProperties() );

        List<Repository> repos = settingsProfile.getRepositories();
        if ( repos != null )
        {
            for ( Repository repo : repos )
            {
                profile.addRepository( convertFromSettingsRepository( repo ) );
            }
        }

        List<Repository> pluginRepos = settingsProfile.getPluginRepositories();
        if ( pluginRepos != null )
        {
            for ( Repository pluginRepo : pluginRepos )
            {
                profile.addPluginRepository( convertFromSettingsRepository( pluginRepo ) );
            }
        }

        return profile;
    }

    /**
     * @param settingsRepo
     * @return a repository
     */
    private static org.apache.maven.model.Repository convertFromSettingsRepository( Repository settingsRepo )
    {
        org.apache.maven.model.Repository repo = new org.apache.maven.model.Repository();

        repo.setId( settingsRepo.getId() );
        repo.setLayout( settingsRepo.getLayout() );
        repo.setName( settingsRepo.getName() );
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

    /**
     * @param settingsPolicy
     * @return a RepositoryPolicy
     */
    private static org.apache.maven.model.RepositoryPolicy convertRepositoryPolicy( RepositoryPolicy settingsPolicy )
    {
        org.apache.maven.model.RepositoryPolicy policy = new org.apache.maven.model.RepositoryPolicy();
        policy.setEnabled( settingsPolicy.isEnabled() );
        policy.setUpdatePolicy( settingsPolicy.getUpdatePolicy() );
        policy.setChecksumPolicy( settingsPolicy.getChecksumPolicy() );
        return policy;
    }

    /**
     * @param settings could be null
     * @return a new instance of settings or null if settings was null.
     */
    public static Settings copySettings( Settings settings )
    {
        if ( settings == null )
        {
            return null;
        }

        Settings clone = new Settings();
        clone.setActiveProfiles( settings.getActiveProfiles() );
        clone.setInteractiveMode( settings.isInteractiveMode() );
        clone.setLocalRepository( settings.getLocalRepository() );
        clone.setMirrors( settings.getMirrors() );
        clone.setModelEncoding( settings.getModelEncoding() );
        clone.setOffline( settings.isOffline() );
        clone.setPluginGroups( settings.getPluginGroups() );
        clone.setProfiles( settings.getProfiles() );
        clone.setProxies( settings.getProxies() );
        clone.setServers( settings.getServers() );
        clone.setSourceLevel( settings.getSourceLevel() );
        clone.setUsePluginRegistry( settings.isUsePluginRegistry() );

        return clone;
    }
}
