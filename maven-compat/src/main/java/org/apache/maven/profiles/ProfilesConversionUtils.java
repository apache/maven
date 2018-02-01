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
 *  http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */

import org.apache.maven.model.Activation;
import org.apache.maven.model.ActivationFile;
import org.apache.maven.model.ActivationProperty;
import org.apache.maven.model.Profile;
import org.apache.maven.model.Repository;

import java.util.List;

/**
 * ProfilesConversionUtils
 */
@Deprecated
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

            activation.setActiveByDefault( profileActivation.isActiveByDefault() );

            activation.setJdk( profileActivation.getJdk() );

            org.apache.maven.profiles.ActivationProperty profileProp = profileActivation.getProperty();

            if ( profileProp != null )
            {
                ActivationProperty prop = new ActivationProperty();

                prop.setName( profileProp.getName() );
                prop.setValue( profileProp.getValue() );

                activation.setProperty( prop );
            }


            ActivationOS profileOs = profileActivation.getOs();

            if ( profileOs != null )
            {
                org.apache.maven.model.ActivationOS os = new org.apache.maven.model.ActivationOS();

                os.setArch( profileOs.getArch() );
                os.setFamily( profileOs.getFamily() );
                os.setName( profileOs.getName() );
                os.setVersion( profileOs.getVersion() );

                activation.setOs( os );
            }

            org.apache.maven.profiles.ActivationFile profileFile = profileActivation.getFile();

            if ( profileFile != null )
            {
                ActivationFile file = new ActivationFile();

                file.setExists( profileFile.getExists() );
                file.setMissing( profileFile.getMissing() );

                activation.setFile( file );
            }

            profile.setActivation( activation );
        }

        profile.setProperties( profileXmlProfile.getProperties() );

        List repos = profileXmlProfile.getRepositories();
        if ( repos != null )
        {
            for ( Object repo : repos )
            {
                profile.addRepository( convertFromProfileXmlRepository( (org.apache.maven.profiles.Repository) repo ) );
            }
        }

        List pluginRepos = profileXmlProfile.getPluginRepositories();
        if ( pluginRepos != null )
        {
            for ( Object pluginRepo : pluginRepos )
            {
                profile.addPluginRepository(
                    convertFromProfileXmlRepository( (org.apache.maven.profiles.Repository) pluginRepo ) );
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
        repo.setUrl( profileXmlRepo.getUrl() );

        if ( profileXmlRepo.getSnapshots() != null )
        {
            repo.setSnapshots( convertRepositoryPolicy( profileXmlRepo.getSnapshots() ) );
        }
        if ( profileXmlRepo.getReleases() != null )
        {
            repo.setReleases( convertRepositoryPolicy( profileXmlRepo.getReleases() ) );
        }

        return repo;
    }

    private static org.apache.maven.model.RepositoryPolicy convertRepositoryPolicy( RepositoryPolicy profileXmlRepo )
    {
        org.apache.maven.model.RepositoryPolicy policy = new org.apache.maven.model.RepositoryPolicy();
        policy.setEnabled( profileXmlRepo.isEnabled() );
        policy.setUpdatePolicy( profileXmlRepo.getUpdatePolicy() );
        policy.setChecksumPolicy( profileXmlRepo.getChecksumPolicy() );
        return policy;
    }

}
