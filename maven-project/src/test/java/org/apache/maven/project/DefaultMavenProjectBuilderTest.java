package org.apache.maven.project;

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

import org.apache.maven.artifact.repository.ArtifactRepository;
import org.apache.maven.artifact.repository.DefaultArtifactRepository;
import org.apache.maven.artifact.repository.layout.ArtifactRepositoryLayout;
import org.apache.maven.model.Build;
import org.apache.maven.model.Plugin;
import org.apache.maven.model.Profile;
import org.apache.maven.model.Repository;
import org.apache.maven.model.Resource;
import org.apache.maven.profiles.DefaultProfileManager;
import org.apache.maven.profiles.ProfileManager;
import org.codehaus.plexus.util.FileUtils;

import java.io.File;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Properties;

public class DefaultMavenProjectBuilderTest
    extends AbstractMavenProjectTestCase
{

    private List filesToDelete = new ArrayList();

    private File localRepoDir;

    public void setUp()
        throws Exception
    {
        super.setUp();

        localRepoDir = new File( System.getProperty( "java.io.tmpdir" ), "local-repo." + System.currentTimeMillis() );
        localRepoDir.mkdirs();

        filesToDelete.add( localRepoDir );
    }

    public void tearDown()
        throws Exception
    {
        super.tearDown();

        if ( !filesToDelete.isEmpty() )
        {
            for ( Iterator it = filesToDelete.iterator(); it.hasNext(); )
            {
                File file = (File) it.next();

                if ( file.exists() )
                {
                    if ( file.isDirectory() )
                    {
                        FileUtils.deleteDirectory( file );
                    }
                    else
                    {
                        file.delete();
                    }
                }
            }
        }
    }

    public void testShouldInjectOneProfileToStandaloneSuperPom()
        throws Exception
    {
        ProfileManager pm = new DefaultProfileManager( getContainer(), new Properties() );

        String profileId = "test-profile";
        String key = "test";
        String value = "value";

        Profile profile = new Profile();
        profile.setId( profileId );
        profile.addProperty( key, value );

        pm.addProfile( profile );
        pm.explicitlyActivate( profileId );

        MavenProject project = projectBuilder.buildStandaloneSuperProject( getLocalRepository(), pm );

        assertEquals( value, project.getProperties().getProperty( key ) );
    }

    public void testShouldInjectProfileWithRepositoryToStandaloneSuperPom()
        throws Exception
    {
        ProfileManager pm = new DefaultProfileManager( getContainer(), new Properties() );

        String profileId = "test-profile";
        String repoId = "test-repo";

        Profile profile = new Profile();
        profile.setId( profileId );

        Repository repo = new Repository();
        repo.setId( repoId );
        repo.setUrl( "http://www.google.com" );

        profile.addRepository( repo );

        pm.addProfile( profile );
        pm.explicitlyActivate( profileId );

        MavenProject project = projectBuilder.buildStandaloneSuperProject( getLocalRepository(), pm );

        List repositories = project.getRepositories();

        assertNotNull( repositories );

        Repository result = null;

        for ( Iterator it = repositories.iterator(); it.hasNext(); )
        {
            Repository candidate = (Repository) it.next();

            if ( repoId.equals( candidate.getId() ) )
            {
                result = candidate;
                break;
            }
        }

        assertNotNull( "Profile-injected repository not found in super-POM.", result );

        assertEquals( "Profile-injected repository was not first in repo list within super-POM", repoId,
                      ( (Repository) repositories.get( 0 ) ).getId() );
    }

    /**
     * Check that we can build ok from the middle pom of a (parent,child,grandchild) heirarchy
     * @throws Exception
     */
    public void testBuildFromMiddlePom() throws Exception
    {
        File f1 = getTestFile( "src/test/resources/projects/grandchild-check/child/pom.xml");
        File f2 = getTestFile( "src/test/resources/projects/grandchild-check/child/grandchild/pom.xml");

        getProject( f1 );

        // it's the building of the grandchild project, having already cached the child project
        // (but not the parent project), which causes the problem.
        getProject( f2 );
    }

     public void testDuplicatePluginDefinitionsMerged()
         throws Exception
     {
         File f1 = getTestFile( "src/test/resources/projects/duplicate-plugins-merged-pom.xml" );

         MavenProject project = getProject( f1 );

         assertEquals( 2, ( (Plugin) project.getBuildPlugins().get( 0 ) ).getDependencies().size() );
     }

     public void testBuildDirectoryExpressionInterpolatedWithTranslatedValue()
        throws Exception
     {
         File pom = getTestFile( "src/test/resources/projects/build-path-expression-pom.xml" );

         MavenProject project = getProject( pom );
         projectBuilder.calculateConcreteState( project, new DefaultProjectBuilderConfiguration() );

         Build build = project.getBuild();
         assertNotNull( "Project should have a build section containing the test resource.", build );

         String sourceDirectory = build.getSourceDirectory();
         assertNotNull( "Project build should contain a valid source directory.", sourceDirectory );

         List resources = build.getResources();
         assertNotNull( "Project should contain a build resource.", resources );
         assertEquals( "Project should contain exactly one build resource.", 1, resources.size() );

         Resource res = (Resource) resources.get( 0 );
         assertEquals( "Project resource should be the same directory as the source directory.", sourceDirectory, res.getDirectory() );

         System.out.println( "Interpolated, translated resource directory is: " + res.getDirectory() );
     }

    protected ArtifactRepository getLocalRepository()
        throws Exception
    {
        ArtifactRepositoryLayout repoLayout = (ArtifactRepositoryLayout) lookup( ArtifactRepositoryLayout.ROLE,
                                                                                 "legacy" );

        ArtifactRepository r = new DefaultArtifactRepository( "local", "file://" + localRepoDir.getAbsolutePath(),
                                                              repoLayout );

        return r;
    }
}
