package org.apache.maven.settings;

import org.apache.maven.model.Activation;
import org.apache.maven.model.ActivationProperty;
import org.apache.maven.model.Profile;
import org.apache.maven.model.Repository;

import java.util.Iterator;
import java.util.List;

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

public final class SettingsConversionUtils
{

    public static Profile convertFromSettingsProfile( org.apache.maven.settings.Profile settingsProfile )
    {
        Profile profile = new Profile();
        
        profile.setId( settingsProfile.getId() );
        
        profile.setSource( "settings.xml" );

        org.apache.maven.settings.Activation settingsActivation = settingsProfile.getActivation();

        if ( settingsActivation != null )
        {
            Activation activation = new Activation();

            activation.setJdk( settingsActivation.getJdk() );

            org.apache.maven.settings.ActivationProperty settingsProp = settingsActivation.getProperty();

            if ( settingsProp != null )
            {
                ActivationProperty prop = new ActivationProperty();

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
                profile
                    .addRepository( convertFromSettingsRepository( (org.apache.maven.settings.Repository) it.next() ) );
            }
        }

        List pluginRepos = settingsProfile.getPluginRepositories();
        if ( pluginRepos != null )
        {
            for ( Iterator it = pluginRepos.iterator(); it.hasNext(); )
            {
                profile.addPluginRepository( convertFromSettingsRepository( (org.apache.maven.settings.Repository) it
                    .next() ) );
            }
        }

        return profile;
    }

    private static Repository convertFromSettingsRepository( org.apache.maven.settings.Repository settingsRepo )
    {
        Repository repo = new Repository();

        repo.setId( settingsRepo.getId() );
        repo.setLayout( settingsRepo.getLayout() );
        repo.setName( settingsRepo.getName() );
        repo.setSnapshotPolicy( settingsRepo.getSnapshotPolicy() );
        repo.setUrl( settingsRepo.getUrl() );

        return repo;
    }

}
