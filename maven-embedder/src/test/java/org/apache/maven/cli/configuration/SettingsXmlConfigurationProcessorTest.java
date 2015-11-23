package org.apache.maven.cli.configuration;

/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */

import org.apache.maven.artifact.repository.ArtifactRepository;
import org.apache.maven.execution.DefaultMavenExecutionRequest;
import org.apache.maven.execution.MavenExecutionRequest;
import org.apache.maven.settings.Mirror;
import org.apache.maven.settings.Profile;
import org.apache.maven.settings.Repository;
import org.apache.maven.settings.Settings;
import org.eclipse.sisu.launch.InjectedTestCase;

import javax.inject.Inject;
import java.util.List;

public class SettingsXmlConfigurationProcessorTest
    extends InjectedTestCase
{
    private static final String A_GLOBAL_MIRROR_ID = "a-global-mirror-id";
    private static final String A_PROFILE_MIRROR_ID = "a-profile-mirror-id";

    @Inject
    SettingsXmlConfigurationProcessor configurationProcessor;
    private MavenExecutionRequest request;
    private Settings settings;

    @Override
    public void setUp() throws Exception
    {
        super.setUp();
        request = new DefaultMavenExecutionRequest();
        settings = new Settings();
    }

    public void testPluginRepositoryInjection()
        throws Exception
    {
        Repository r = new Repository();
        r.setId( "test" );
        r.setUrl( "file:///test" );

        Profile p = new Profile();
        p.setId( "test" );
        p.addPluginRepository( r );

        settings.addProfile( p );
        settings.addActiveProfile( p.getId() );

        configurationProcessor.populateFromSettings( request, settings );

        List<ArtifactRepository> repositories = request.getPluginArtifactRepositories();
        assertEquals( 1, repositories.size() );
        assertEquals( r.getId(), repositories.get( 0 ).getId() );
        assertEquals( r.getUrl(), repositories.get( 0 ).getUrl() );
    }

    public void testMirrorsFromGlobalSettingsAreCloned()
        throws Exception
    {
        Mirror aGivenMirror = createMirrorWithId( A_GLOBAL_MIRROR_ID );
        settings.addMirror( aGivenMirror );

        configurationProcessor.populateFromSettings( request, settings );

        List<Mirror> mirrors = request.getMirrors();
        assertEquals( 1, mirrors.size() );
        assertNotSame( aGivenMirror, mirrors.get( 0 ) );
        assertEquals( A_GLOBAL_MIRROR_ID, mirrors.get( 0 ).getId() );
    }

    public void testMirrorsFromGlobalSettingsArePopulated()
        throws Exception
    {
        settings.addMirror( createMirrorWithId( A_GLOBAL_MIRROR_ID ) );

        configurationProcessor.populateFromSettings( request, settings );

        List<Mirror> mirrors = request.getMirrors();
        assertEquals( 1, mirrors.size() );
        assertEquals( A_GLOBAL_MIRROR_ID, mirrors.get( 0 ).getId() );
    }

    public void testMirrorsFromProfileSettingsArePopulated()
        throws Exception
    {
        Profile profile = new Profile();
        profile.setId( "aProfile" );
        profile.getMirrors().add( createMirrorWithId( A_GLOBAL_MIRROR_ID ) );

        settings.addProfile( profile );
        settings.addActiveProfile( "aProfile" );

        configurationProcessor.populateFromSettings( request, settings );

        List<Mirror> mirrors = request.getMirrors();
        assertEquals( 1, mirrors.size() );
        assertEquals( A_GLOBAL_MIRROR_ID, mirrors.get( 0 ).getId() );
    }

    public void testMirrorsGlobalAndFromProfileSettingsArePopulated()
        throws Exception
    {
        Profile profile = new Profile();
        profile.setId( "aProfile" );
        profile.getMirrors().add( createMirrorWithId( A_PROFILE_MIRROR_ID ) );

        settings.addMirror( createMirrorWithId( A_GLOBAL_MIRROR_ID ) );
        settings.addProfile( profile );
        settings.addActiveProfile( "aProfile" );

        configurationProcessor.populateFromSettings( request, settings );

        List<Mirror> mirrors = request.getMirrors();
        assertEquals( 2, mirrors.size() );
        assertEquals( A_GLOBAL_MIRROR_ID, mirrors.get( 0 ).getId() );
        assertEquals( A_PROFILE_MIRROR_ID, mirrors.get( 1 ).getId() );
    }

    private Mirror createMirrorWithId( String id )
    {
        Mirror mirror = new Mirror();
        mirror.setId( id );
        return mirror;
    }

}