package org.apache.maven.settings.validation;

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
import java.util.List;

import org.apache.maven.settings.Mirror;
import org.apache.maven.settings.Profile;
import org.apache.maven.settings.Proxy;
import org.apache.maven.settings.Repository;
import org.apache.maven.settings.Server;
import org.apache.maven.settings.Settings;
import org.apache.maven.settings.building.SettingsProblem.Severity;
import org.apache.maven.settings.building.SettingsProblemCollector;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * @author mkleint
 */
public class DefaultSettingsValidatorTest
{

    private DefaultSettingsValidator validator;

    @BeforeEach
    public void setUp()
        throws Exception
    {
        validator = new DefaultSettingsValidator();
    }

    @AfterEach
    public void tearDown()
        throws Exception
    {
        validator = null;
    }

    private void assertContains( String msg, String substring )
    {
        assertTrue( msg.contains( substring ), "\"" + substring + "\" was not found in: " + msg );
    }

    @Test
    public void testValidate()
    {
        Settings model = new Settings();
        Profile prof = new Profile();
        prof.setId( "xxx" );
        model.addProfile( prof );
        SimpleProblemCollector problems = new SimpleProblemCollector();
        validator.validate( model, problems );
        assertEquals( 0, problems.messages.size() );

        Repository repo = new Repository();
        prof.addRepository( repo );
        problems = new SimpleProblemCollector();
        validator.validate( model, problems );
        assertEquals( 2, problems.messages.size() );

        repo.setUrl( "http://xxx.xxx.com" );
        problems = new SimpleProblemCollector();
        validator.validate( model, problems );
        assertEquals( 1, problems.messages.size() );

        repo.setId( "xxx" );
        problems = new SimpleProblemCollector();
        validator.validate( model, problems );
        assertEquals( 0, problems.messages.size() );
    }

    @Test
    public void testValidateMirror()
        throws Exception
    {
        Settings settings = new Settings();
        Mirror mirror = new Mirror();
        mirror.setId( "local" );
        settings.addMirror( mirror );
        mirror = new Mirror();
        mirror.setId( "illegal\\:/chars" );
        mirror.setUrl( "http://void" );
        mirror.setMirrorOf( "void" );
        settings.addMirror( mirror );

        SimpleProblemCollector problems = new SimpleProblemCollector();
        validator.validate( settings, problems );
        assertEquals( 4, problems.messages.size() );
        assertContains( problems.messages.get( 0 ), "'mirrors.mirror.id' must not be 'local'" );
        assertContains( problems.messages.get( 1 ), "'mirrors.mirror.url' for local is missing" );
        assertContains( problems.messages.get( 2 ), "'mirrors.mirror.mirrorOf' for local is missing" );
        assertContains( problems.messages.get( 3 ), "'mirrors.mirror.id' must not contain any of these characters" );
    }

    @Test
    public void testValidateRepository()
        throws Exception
    {
        Profile profile = new Profile();
        Repository repo = new Repository();
        repo.setId( "local" );
        profile.addRepository( repo );
        repo = new Repository();
        repo.setId( "illegal\\:/chars" );
        repo.setUrl( "http://void" );
        profile.addRepository( repo );
        Settings settings = new Settings();
        settings.addProfile( profile );

        SimpleProblemCollector problems = new SimpleProblemCollector();
        validator.validate( settings, problems );
        assertEquals( 3, problems.messages.size() );
        assertContains( problems.messages.get( 0 ),
                        "'profiles.profile[default].repositories.repository.id' must not be 'local'" );
        assertContains( problems.messages.get( 1 ),
                        "'profiles.profile[default].repositories.repository.url' for local is missing" );
        assertContains( problems.messages.get( 2 ),
                        "'profiles.profile[default].repositories.repository.id' must not contain any of these characters" );
    }

    @Test
    public void testValidateUniqueServerId()
        throws Exception
    {
        Settings settings = new Settings();
        Server server1 = new Server();
        server1.setId( "test" );
        settings.addServer( server1 );
        Server server2 = new Server();
        server2.setId( "test" );
        settings.addServer( server2 );

        SimpleProblemCollector problems = new SimpleProblemCollector();
        validator.validate( settings, problems );
        assertEquals( 1, problems.messages.size() );
        assertContains( problems.messages.get( 0 ),
                        "'servers.server.id' must be unique but found duplicate server with id test" );
    }

    @Test
    public void testValidateUniqueProfileId()
        throws Exception
    {
        Settings settings = new Settings();
        Profile profile1 = new Profile();
        profile1.setId( "test" );
        settings.addProfile( profile1 );
        Profile profile2 = new Profile();
        profile2.setId( "test" );
        settings.addProfile( profile2 );

        SimpleProblemCollector problems = new SimpleProblemCollector();
        validator.validate( settings, problems );
        assertEquals( 1, problems.messages.size() );
        assertContains( problems.messages.get( 0 ),
                        "'profiles.profile.id' must be unique but found duplicate profile with id test" );
    }

    @Test
    public void testValidateUniqueRepositoryId()
        throws Exception
    {
        Settings settings = new Settings();
        Profile profile = new Profile();
        profile.setId( "pro" );
        settings.addProfile( profile );
        Repository repo1 = new Repository();
        repo1.setUrl( "http://apache.org/" );
        repo1.setId( "test" );
        profile.addRepository( repo1 );
        Repository repo2 = new Repository();
        repo2.setUrl( "http://apache.org/" );
        repo2.setId( "test" );
        profile.addRepository( repo2 );

        SimpleProblemCollector problems = new SimpleProblemCollector();
        validator.validate( settings, problems );
        assertEquals( 1, problems.messages.size() );
        assertContains( problems.messages.get( 0 ), "'profiles.profile[pro].repositories.repository.id' must be unique"
            + " but found duplicate repository with id test" );
    }

    @Test
    public void testValidateUniqueProxyId()
        throws Exception
    {
        Settings settings = new Settings();
        Proxy proxy = new Proxy();
        String id = null;
        proxy.setId( id );
        proxy.setHost("www.example.com");
        settings.addProxy( proxy );
        settings.addProxy( proxy );

        SimpleProblemCollector problems = new SimpleProblemCollector();
        validator.validate( settings, problems );
        assertEquals( 1, problems.messages.size() );
        assertContains( problems.messages.get( 0 ), "'proxies.proxy.id' must be unique"
            + " but found duplicate proxy with id " + id );

    }

    @Test
    public void testValidateProxy()
        throws Exception
    {
        Settings settings = new Settings();
        Proxy proxy1 = new Proxy();
        settings.addProxy( proxy1 );

        SimpleProblemCollector problems = new SimpleProblemCollector();
        validator.validate( settings, problems );
        assertEquals( 1, problems.messages.size() );
        assertContains( problems.messages.get( 0 ), "'proxies.proxy.host' for default is missing" );
    }

    private static class SimpleProblemCollector
        implements SettingsProblemCollector
    {

        public List<String> messages = new ArrayList<>();

        public void add( Severity severity, String message, int line, int column, Exception cause )
        {
            messages.add( message );
        }

    }

}
