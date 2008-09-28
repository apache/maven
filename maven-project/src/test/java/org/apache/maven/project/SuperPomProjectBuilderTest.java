package org.apache.maven.project;

import org.apache.maven.artifact.repository.ArtifactRepository;
import org.apache.maven.model.Profile;
import org.apache.maven.model.Repository;
import org.apache.maven.profiles.DefaultProfileManager;
import org.apache.maven.profiles.ProfileManager;
import org.apache.maven.profiles.activation.DefaultProfileActivationContext;

import java.util.Iterator;
import java.util.List;
import java.util.Properties;

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

public class SuperPomProjectBuilderTest
    extends AbstractMavenProjectTestCase
{
    public void setUp()
        throws Exception
    {
        super.setUp();

        projectBuilder = (DefaultMavenProjectBuilder) lookup( MavenProjectBuilder.ROLE );
    }

    public void testStandaloneSuperPomContainsInjectedExternalProfileRepositories()
        throws Exception
    {
        Profile profile = new Profile();
        profile.setId( "test-profile" );

        Repository repo = new Repository();
        repo.setId( "test" );
        repo.setUrl( "http://www.nowhere.com" );

        profile.addRepository( repo );

        ProfileManager pm = new DefaultProfileManager( getContainer(), new DefaultProfileActivationContext( new Properties(), true ) );

        pm.addProfile( profile );
        pm.explicitlyActivate( profile.getId() );

        ProjectBuilderConfiguration pbc = new DefaultProjectBuilderConfiguration();
        pbc.setGlobalProfileManager( pm );
        MavenProject project = projectBuilder.buildStandaloneSuperProject( pbc );

        assertRepository( repo.getId(), project.getRepositories() );
        assertRepository( repo.getId(), project.getPluginRepositories() );
        assertArtifactRepository( repo.getId(), project.getRemoteArtifactRepositories() );
        assertArtifactRepository( repo.getId(), project.getPluginArtifactRepositories() );
    }

    public void testStandaloneSuperPomContainsCentralRepo()
        throws ProjectBuildingException
    {
        MavenProject project = projectBuilder.buildStandaloneSuperProject( new DefaultProjectBuilderConfiguration() );

        assertRepository( "central", project.getRepositories() );
        assertRepository( "central", project.getPluginRepositories() );
        assertArtifactRepository( "central", project.getRemoteArtifactRepositories() );
        assertArtifactRepository( "central", project.getPluginArtifactRepositories() );
    }

    private void assertArtifactRepository( String id,
                                           List repos )
    {
        assertNotNull( repos );
        assertFalse( repos.isEmpty() );

        boolean found = false;
        for ( Iterator it = repos.iterator(); it.hasNext(); )
        {
            ArtifactRepository repo = (ArtifactRepository) it.next();

            found = id.equals( repo.getId() );
            if ( found )
            {
                break;
            }
        }

        assertTrue( found );
    }

    private void assertRepository( String id,
                                   List repos )
    {
        assertNotNull( repos );
        assertFalse( repos.isEmpty() );

        boolean found = false;
        for ( Iterator it = repos.iterator(); it.hasNext(); )
        {
            Repository repo = (Repository) it.next();

            found = id.equals( repo.getId() );
            if ( found )
            {
                break;
            }
        }

        assertTrue( found );
    }

}