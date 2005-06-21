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

import org.apache.maven.artifact.repository.DefaultArtifactRepository;
import org.apache.maven.artifact.repository.ArtifactRepository;
import org.apache.maven.artifact.repository.layout.ArtifactRepositoryLayout;
import org.codehaus.plexus.PlexusTestCase;

import java.io.File;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

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

        parameters.put( "outputDirectory", new File( getBasedir(), "target/archetype" ).getPath() );

        // ----------------------------------------------------------------------
        // This needs to be encapsulated in a maven test case.
        // ----------------------------------------------------------------------

        ArtifactRepositoryLayout layout = (ArtifactRepositoryLayout) container.lookup( ArtifactRepositoryLayout.ROLE,
                                                                                       "legacy" );

        String mavenRepoLocal = getTestFile( "target/local-repository" ).toURL().toString();
        ArtifactRepository localRepository = new DefaultArtifactRepository( "local", mavenRepoLocal, layout );

        List remoteRepositories = new ArrayList();

        String mavenRepoRemote = getTestFile( "src/test/repository" ).toURL().toString();
        ArtifactRepository remoteRepository = new DefaultArtifactRepository( "remote", mavenRepoRemote, layout );

        remoteRepositories.add( remoteRepository );

        archetype.createArchetype( "org.apache.maven.archetypes", "maven-archetype-quickstart", "1.0-alpha-1-SNAPSHOT",
                                   localRepository, remoteRepositories, parameters );

        // TODO: validate output
    }
}
