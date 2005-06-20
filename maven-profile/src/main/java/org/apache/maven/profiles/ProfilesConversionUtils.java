package org.apache.maven.profiles;

import org.apache.maven.model.Activation;
import org.apache.maven.model.ActivationProperty;
import org.apache.maven.model.Profile;
import org.apache.maven.model.Repository;
import org.apache.maven.profiles.AlwaysOnActivation;

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

public class ProfilesConversionUtils
{
    
    private ProfilesConversionUtils()
    {
    }

    public static Profile convertFromProfileXmlProfile( org.apache.maven.profiles.Profile profileXmlProfile )
    {
        Profile profile = new Profile();
        
        profile.setId( profileXmlProfile.getId() );
        
        profile.setSource( "profiles.xml" );

        org.apache.maven.profiles.Activation profileActivation = profileXmlProfile.getActivation();

        if ( profileActivation != null )
        {
            Activation activation = new Activation();

            activation.setJdk( profileActivation.getJdk() );

            org.apache.maven.profiles.ActivationProperty profileProp = profileActivation.getProperty();

            if ( profileProp != null )
            {
                ActivationProperty prop = new ActivationProperty();

                prop.setName( profileProp.getName() );
                prop.setValue( profileProp.getValue() );

                activation.setProperty( prop );
            }
            
            profile.setActivation( activation );
        }
        else
        {
            profile.setActivation( new AlwaysOnActivation() );
        }
        
        profile.setProperties( profileXmlProfile.getProperties() );

        List repos = profileXmlProfile.getRepositories();
        if ( repos != null )
        {
            for ( Iterator it = repos.iterator(); it.hasNext(); )
            {
                profile
                    .addRepository( convertFromProfileXmlRepository( (org.apache.maven.profiles.Repository) it.next() ) );
            }
        }

        List pluginRepos = profileXmlProfile.getPluginRepositories();
        if ( pluginRepos != null )
        {
            for ( Iterator it = pluginRepos.iterator(); it.hasNext(); )
            {
                profile.addPluginRepository( convertFromProfileXmlRepository( (org.apache.maven.profiles.Repository) it
                    .next() ) );
            }
        }

        return profile;
    }

    private static Repository convertFromProfileXmlRepository( org.apache.maven.profiles.Repository profileXmlRepo )
    {
        Repository repo = new Repository();

        repo.setId( profileXmlRepo.getId() );
        repo.setLayout( profileXmlRepo.getLayout() );
        repo.setName( profileXmlRepo.getName() );
        repo.setSnapshotPolicy( profileXmlRepo.getSnapshotPolicy() );
        repo.setChecksumPolicy( profileXmlRepo.getChecksumPolicy() );
        repo.setUrl( profileXmlRepo.getUrl() );

        return repo;
    }

}
