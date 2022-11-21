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
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.Random;

import org.apache.maven.api.settings.Activation;
import org.apache.maven.api.settings.ActivationFile;
import org.apache.maven.api.settings.ActivationOS;
import org.apache.maven.api.settings.ActivationProperty;
import org.apache.maven.api.settings.Profile;
import org.apache.maven.api.settings.Repository;
import org.apache.maven.api.settings.Settings;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

public class SettingsUtilsTest
{

    @Test
    public void testShouldAppendRecessivePluginGroupIds()
    {
        Settings dominant = Settings.newBuilder()
                .pluginGroups( Arrays.asList( "org.apache.maven.plugins", "org.codehaus.modello" ) )
                .build();

        Settings recessive = Settings.newBuilder()
                .pluginGroups( Arrays.asList( "org.codehaus.plexus" ) )
                .build();

        Settings merged = SettingsUtilsV4.merge( dominant, recessive, Settings.GLOBAL_LEVEL );

        List<String> pluginGroups = merged.getPluginGroups();

        assertNotNull( pluginGroups );
        assertEquals( 3, pluginGroups.size() );
        assertEquals( "org.apache.maven.plugins", pluginGroups.get( 0 ) );
        assertEquals( "org.codehaus.modello", pluginGroups.get( 1 ) );
        assertEquals( "org.codehaus.plexus", pluginGroups.get( 2 ) );
    }

    @Test
    public void testRoundTripProfiles()
    {
        Random entropy = new Random();
        ActivationFile af = ActivationFile.newBuilder()
                .exists( "exists" + Long.toHexString( entropy.nextLong() ) )
                .missing( "missing" + Long.toHexString( entropy.nextLong() ) )
                .build();
        ActivationProperty ap = ActivationProperty.newBuilder()
                .name( "name" + Long.toHexString( entropy.nextLong() ) )
                .value( "value" + Long.toHexString( entropy.nextLong() ) )
                .build();
        ActivationOS ao = ActivationOS.newBuilder()
                .arch( "arch" + Long.toHexString( entropy.nextLong() ) )
                .family( "family" + Long.toHexString( entropy.nextLong() ) )
                .name( "name" + Long.toHexString( entropy.nextLong() ) )
                .version( "version" + Long.toHexString( entropy.nextLong() ) )
                .build();
        Activation a = Activation.newBuilder()
                .activeByDefault( entropy.nextBoolean() )
                .jdk( "jdk" + Long.toHexString( entropy.nextLong() ) )
                .file( af )
                .property( ap )
                .os( ao )
                .build();
        Map<String, String> props = new HashMap<>();
        int count = entropy.nextInt( 10 );
        for ( int i = 0; i < count; i++ )
        {
            props.put( "name" + Long.toHexString( entropy.nextLong() ),
                    "value" + Long.toHexString( entropy.nextLong() ) );
        }
        count = entropy.nextInt( 3 );
        List<Repository> repos = new ArrayList<>();
        for ( int i = 0; i < count; i++ )
        {
            Repository r = Repository.newBuilder()
                    .id( "id" + Long.toHexString( entropy.nextLong() ) )
                    .name( "name" + Long.toHexString( entropy.nextLong() ) )
                    .url(  "url" + Long.toHexString( entropy.nextLong() ) )
                    .build();
            repos.add( r );
        }
        count = entropy.nextInt( 3 );
        List<Repository> pluginRepos = new ArrayList<>();
        for ( int i = 0; i < count; i++ )
        {
            Repository r = Repository.newBuilder()
                    .id( "id" + Long.toHexString( entropy.nextLong() ) )
                    .name( "name" + Long.toHexString( entropy.nextLong() ) )
                    .url(  "url" + Long.toHexString( entropy.nextLong() ) )
                    .build();
            pluginRepos.add( r );
        }
        Profile p = Profile.newBuilder()
                .id( "id" + Long.toHexString( entropy.nextLong() ) )
                .activation( a )
                .properties( props )
                .repositories( repos )
                .pluginRepositories( pluginRepos )
                .build();

        Profile clone = SettingsUtilsV4.convertToSettingsProfile( SettingsUtilsV4.convertFromSettingsProfile( p ) );

        assertEquals( p.getId(), clone.getId() );
        assertEquals( p.getActivation().getJdk(), clone.getActivation().getJdk() );
        assertEquals( p.getActivation().getFile().getExists(), clone.getActivation().getFile().getExists() );
        assertEquals( p.getActivation().getFile().getMissing(), clone.getActivation().getFile().getMissing() );
        assertEquals( p.getActivation().getProperty().getName(), clone.getActivation().getProperty().getName() );
        assertEquals( p.getActivation().getProperty().getValue(), clone.getActivation().getProperty().getValue() );
        assertEquals( p.getActivation().getOs().getArch(), clone.getActivation().getOs().getArch() );
        assertEquals( p.getActivation().getOs().getFamily(), clone.getActivation().getOs().getFamily() );
        assertEquals( p.getActivation().getOs().getName(), clone.getActivation().getOs().getName() );
        assertEquals( p.getActivation().getOs().getVersion(), clone.getActivation().getOs().getVersion() );
        assertEquals( p.getProperties(), clone.getProperties() );
        assertEquals( p.getRepositories().size(), clone.getRepositories().size() );
        // TODO deep compare the lists
        assertEquals( p.getPluginRepositories().size(), clone.getPluginRepositories().size() );
        // TODO deep compare the lists
    }

}