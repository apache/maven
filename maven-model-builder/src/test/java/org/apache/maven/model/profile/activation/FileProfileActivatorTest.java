package org.apache.maven.model.profile.activation;

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

import org.apache.maven.model.Activation;
import org.apache.maven.model.ActivationFile;
import org.apache.maven.model.Profile;
import org.apache.maven.model.path.DefaultPathTranslator;
import org.apache.maven.model.path.ProfileActivationFilePathInterpolator;
import org.apache.maven.model.profile.DefaultProfileActivationContext;
import org.junit.jupiter.api.Test;

import java.io.File;
import java.io.IOException;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * Tests {@link FileProfileActivator}.
 *
 * @author Ravil Galeyev
 */
public class FileProfileActivatorTest extends AbstractProfileActivatorTest<FileProfileActivator>
{
    private static final String PATH = "src/test/resources/";
    private static final String FILE = "file.txt";

    private final DefaultProfileActivationContext context = new DefaultProfileActivationContext();

    public FileProfileActivatorTest()
    {
        super( FileProfileActivator.class );
    }

    @Override
    public void setUp() throws Exception
    {
        super.setUp();
        activator.setProfileActivationFilePathInterpolator(
                new ProfileActivationFilePathInterpolator().setPathTranslator( new DefaultPathTranslator() ) );

        context.setProjectDirectory( new File( PATH ) );

        File file = new File( PATH + FILE );
        if ( !file.exists() )
        {
            if ( !file.createNewFile() )
            {
                throw new IOException( "Can't create " + file );
            }
        }
    }


    @Test
    public void test_isActive_noFile()
    {
        assertActivation( false, newExistsProfile( null ), context );
        assertActivation( false, newExistsProfile( "someFile.txt" ), context );
        assertActivation( false, newExistsProfile( "${basedir}/someFile.txt" ), context );

        assertActivation( false, newMissingProfile( null ), context );
        assertActivation( true, newMissingProfile( "someFile.txt" ), context );
        assertActivation( true, newMissingProfile( "${basedir}/someFile.txt" ), context );
    }

    @Test
    public void test_isActiveExists_fileExists()
    {
        assertActivation( true, newExistsProfile( FILE ), context );
        assertActivation( true, newExistsProfile( "${basedir}" ), context );
        assertActivation( true, newExistsProfile( "${basedir}/" + FILE ), context );

        assertActivation( false, newMissingProfile( FILE ), context );
        assertActivation( false, newMissingProfile( "${basedir}" ), context );
        assertActivation( false, newMissingProfile( "${basedir}/" + FILE ), context );
    }

    @Test
    public void test_isActiveExists_leavesFileUnchanged()
    {
        Profile profile = newExistsProfile( FILE );
        assertEquals( profile.getActivation().getFile().getExists(), FILE );

        assertActivation( true, profile, context );

        assertEquals( profile.getActivation().getFile().getExists(), FILE );
    }

    private Profile newExistsProfile( String filePath )
    {
        return newProfile( filePath, true );
    }

    private Profile newMissingProfile( String filePath )
    {
        return newProfile( filePath, false );
    }

    private Profile newProfile( String filePath, boolean exists )
    {
        ActivationFile activationFile = new ActivationFile();
        if ( exists )
        {
            activationFile.setExists( filePath );
        }
        else
        {
            activationFile.setMissing( filePath );
        }

        Activation activation = new Activation();
        activation.setFile( activationFile );

        Profile profile = new Profile();
        profile.setActivation( activation );

        return profile;
    }

    @Override
    public void tearDown() throws Exception
    {
        File file = new File( PATH + FILE );
        if ( file.exists() )
        {
            if ( !file.delete() )
            {
                throw new IOException( "Can't delete " + file );
            }
        }
        super.tearDown();
    }
}
