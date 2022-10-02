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
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import org.apache.maven.api.settings.Mirror;
import org.apache.maven.api.settings.Profile;
import org.apache.maven.api.settings.Proxy;
import org.apache.maven.api.settings.Repository;
import org.apache.maven.api.settings.Server;
import org.apache.maven.api.settings.Settings;
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
        Profile prof = Profile.newInstance().withId( "xxx" );
        Settings model = Settings.newInstance().withProfiles( Collections.singletonList( prof ) );
        SimpleProblemCollector problems = new SimpleProblemCollector();
        validator.validate( model, problems );
        assertEquals( 0, problems.messages.size() );

        Repository repo = Repository.newInstance();
        prof = prof.withRepositories( Collections.singletonList( repo ) );
        model = model.withProfiles( Collections.singletonList( prof ) );
        problems = new SimpleProblemCollector();
        validator.validate( model, problems );
        assertEquals( 2, problems.messages.size() );

        repo = repo.withUrl( "http://xxx.xxx.com" );
        prof = prof.withRepositories( Collections.singletonList( repo ) );
        model = model.withProfiles( Collections.singletonList( prof ) );
        problems = new SimpleProblemCollector();
        validator.validate( model, problems );
        assertEquals( 1, problems.messages.size() );

        repo = repo.withId( "xxx" );
        prof = prof.withRepositories( Collections.singletonList( repo ) );
        model = model.withProfiles( Collections.singletonList( prof ) );
        problems = new SimpleProblemCollector();
        validator.validate( model, problems );
        assertEquals( 0, problems.messages.size() );
    }

    @Test
    public void testValidateMirror()
        throws Exception
    {
        Mirror mirror1 = Mirror.newBuilder().id( "local" ).build();
        Mirror mirror2 = Mirror.newBuilder().id( "illegal\\:/chars" ).url( "http://void" ).mirrorOf( "void" ).build();
        Settings settings = Settings.newBuilder().mirrors( Arrays.asList( mirror1, mirror2 ) ).build();

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
        Repository repo1 = Repository.newBuilder().id( "local" ).build();
        Repository repo2 = Repository.newBuilder().id( "illegal\\:/chars" ).url( "http://void" ).build();
        Profile profile = Profile.newBuilder().repositories( Arrays.asList( repo1, repo2 ) ).build();
        Settings settings = Settings.newBuilder().profiles( Collections.singletonList( profile ) ).build();

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
        Server server1 = Server.newBuilder().id( "test" ).build();
        Server server2 = Server.newBuilder().id( "test" ).build();
        Settings settings = Settings.newBuilder().servers( Arrays.asList( server1, server2 ) ).build();

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
        Profile profile1 = Profile.newBuilder().id( "test" ).build();
        Profile profile2 = Profile.newBuilder().id( "test" ).build();
        Settings settings = Settings.newBuilder().profiles( Arrays.asList( profile1, profile2 ) ).build();

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
        Repository repo1 = Repository.newBuilder().id( "test" ).url( "http://apache.org/" ).build();
        Repository repo2 = Repository.newBuilder().id( "test" ).url( "http://apache.org/" ).build();
        Profile profile = Profile.newBuilder().id( "pro" ).repositories( Arrays.asList( repo1, repo2 ) ).build();
        Settings settings = Settings.newBuilder().profiles( Collections.singletonList( profile ) ).build();

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
        Proxy proxy = Proxy.newBuilder().id( "foo" ).host( "www.example.com" ).build();
        Settings settings = Settings.newBuilder().proxies( Arrays.asList( proxy, proxy ) ).build();

        SimpleProblemCollector problems = new SimpleProblemCollector();
        validator.validate( settings, problems );
        assertEquals( 1, problems.messages.size() );
        assertContains( problems.messages.get( 0 ), "'proxies.proxy.id' must be unique"
                + " but found duplicate proxy with id foo" );

    }

    @Test
    public void testValidateUniqueProxyNullId()
            throws Exception
    {
        Proxy proxy = Proxy.newBuilder( false ).host( "www.example.com" ).build();
        Settings settings = Settings.newBuilder().proxies( Arrays.asList( proxy, proxy ) ).build();

        SimpleProblemCollector problems = new SimpleProblemCollector();
        validator.validate( settings, problems );
        assertEquals( 1, problems.messages.size() );
        assertContains( problems.messages.get( 0 ), "'proxies.proxy.id' must be unique"
                + " but found duplicate proxy with id null" );

    }

    @Test
    public void testValidateProxy()
        throws Exception
    {
        Proxy proxy = Proxy.newBuilder().build();
        Settings settings = Settings.newBuilder().proxies( Collections.singletonList( proxy ) ).build();

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
