/*
 * Copyright 2004 Carlos Sanchez.
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
package org.apache.maven.repository;

import java.io.File;
import java.util.List;

import org.apache.maven.artifact.Artifact;
import org.apache.maven.artifact.ArtifactComponentTestCase;
import org.apache.maven.repository.RepositoryTools;

/**
 * @author Carlos Sanchez
 * @version $Revision$
 */
public class RepositoryToolsTest
    extends ArtifactComponentTestCase
{

    protected void setUp() throws Exception
    {
        super.setUp();
    }

    protected String component()
    {
        return "repositorytools";
    }

    public void testGetAllArtifacts() throws Exception
    {
        Artifact artifact = createLocalArtifact( "a", "1.0" );
        List artifacts = RepositoryTools.getAllArtifacts( localRepository() );
        assertEquals( 1, artifacts.size() );
        assertEquals( artifact, artifacts.get( 0 ) );
    }

    public void testGetArtifact()
    {
        File artifactFile = new File( "whatever/maven/jars/maven-meeper-1.0.jar" );
        Artifact artifact = RepositoryTools.getArtifact( "maven", "jar", artifactFile );
        assertEquals( "maven", artifact.getGroupId() );
        assertEquals( "maven-meeper", artifact.getArtifactId() );
        assertEquals( "1.0", artifact.getVersion() );
        assertEquals( "jar", artifact.getType() );
        assertNotNull( artifact.getPath() );
    }

}
