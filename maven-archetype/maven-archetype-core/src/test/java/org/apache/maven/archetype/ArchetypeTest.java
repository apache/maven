package org.apache.maven.archetype;

/*
 * Copyright 2001-2004 The Apache Software Foundation.
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

import org.codehaus.plexus.PlexusTestCase;
import org.apache.maven.artifact.repository.ArtifactRepository;

import java.util.Map;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Set;
import java.util.Properties;
import java.io.File;
import java.io.FileInputStream;

/**
 * @author <a href="mailto:jason@maven.org">Jason van Zyl</a>
 * @version $Id$
 */
public class ArchetypeTest
    extends PlexusTestCase
{
    public void testArchetype()
        throws Exception
    {
        Archetype archetype = (Archetype) lookup( Archetype.ROLE );

        Map parameters = new HashMap();

        parameters.put( "name", "jason" );

        parameters.put( "groupId", "maven" );

        parameters.put( "artifactId", "quickstart" );

        parameters.put( "version", "1.0-alpha-1-SNAPSHOT" );

        parameters.put( "package", "org.apache.maven.quickstart" );

        parameters.put( "outputDirectory",new File( getBasedir(), "target/archetype" ).getPath() );

        // ----------------------------------------------------------------------
        // This needs to be encapsulated in a maven test case.
        // ----------------------------------------------------------------------

        File mavenPropertiesFile = new File( System.getProperty( "user.home" ), ".m2/maven.properties" );

        Properties mavenProperties = new Properties();

        mavenProperties.load( new FileInputStream( mavenPropertiesFile ) );

        ArtifactRepository localRepository = new ArtifactRepository( "local", "file://" + mavenProperties.getProperty( "maven.repo.local" ) );

        Set remoteRepositories = new HashSet();

        ArtifactRepository remoteRepository = new ArtifactRepository( "remote", "http://repo1.maven.org" );

        remoteRepositories.add( remoteRepository );

        archetype.createArchetype( "maven", "maven-archetype-quickstart", "1.0-alpha-1-SNAPSHOT",
                                   localRepository, remoteRepositories, parameters);
    }
}