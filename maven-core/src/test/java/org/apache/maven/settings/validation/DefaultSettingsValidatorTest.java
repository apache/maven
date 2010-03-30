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

import junit.framework.TestCase;

import org.apache.maven.settings.Mirror;
import org.apache.maven.settings.Profile;
import org.apache.maven.settings.Repository;
import org.apache.maven.settings.Settings;
import org.apache.maven.settings.building.SettingsProblemCollector;
import org.apache.maven.settings.building.SettingsProblem.Severity;

/**
 * @author mkleint
 */
public class DefaultSettingsValidatorTest
    extends TestCase
{

    private DefaultSettingsValidator validator;

    protected void setUp()
        throws Exception
    {
        super.setUp();

        validator = new DefaultSettingsValidator();
    }

    protected void tearDown()
        throws Exception
    {
        validator = null;

        super.tearDown();
    }

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

    public void testValidateMirror()
        throws Exception
    {
        Mirror mirror = new Mirror();
        mirror.setId( "local" );
        Settings settings = new Settings();
        settings.addMirror( mirror );

        SimpleProblemCollector problems = new SimpleProblemCollector();
        validator.validate( settings, problems );
        assertEquals( 3, problems.messages.size() );
        assertTrue( problems.messages.get( 0 ), problems.messages.get( 0 ).contains( "'mirrors.mirror.id' must not be 'local'" ) );
        assertTrue( problems.messages.get( 1 ), problems.messages.get( 1 ).contains( "'mirrors.mirror.url' is missing" ) );
        assertTrue( problems.messages.get( 2 ),
                    problems.messages.get( 2 ).contains( "'mirrors.mirror.mirrorOf' is missing" ) );
    }

    public void testValidateRepository()
        throws Exception
    {
        Repository repo = new Repository();
        repo.setId( "local" );
        Profile profile = new Profile();
        profile.addRepository( repo );
        Settings settings = new Settings();
        settings.addProfile( profile );

        SimpleProblemCollector problems = new SimpleProblemCollector();
        validator.validate( settings, problems );
        assertEquals( 2, problems.messages.size() );
        assertTrue( problems.messages.get( 0 ),
                    problems.messages.get( 0 ).contains( "'repositories.repository.id' must not be 'local'" ) );
        assertTrue( problems.messages.get( 1 ),
                    problems.messages.get( 1 ).contains( "'repositories.repository.url' is missing" ) );
    }

    private static class SimpleProblemCollector
        implements SettingsProblemCollector
    {

        public List<String> messages = new ArrayList<String>();

        public void add( Severity severity, String message, int line, int column, Exception cause )
        {
            messages.add( message );
        }

    }

}
