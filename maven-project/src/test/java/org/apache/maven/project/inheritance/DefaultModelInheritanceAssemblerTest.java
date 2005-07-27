package org.apache.maven.project.inheritance;

/*
 * Copyright 2001-2005 The Apache Software Foundation.
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

import junit.framework.TestCase;
import org.apache.maven.model.Build;
import org.apache.maven.model.Goal;
import org.apache.maven.model.Model;
import org.apache.maven.model.Parent;
import org.apache.maven.model.Plugin;
import org.apache.maven.model.Repository;
import org.apache.maven.model.Resource;
import org.apache.maven.model.Scm;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

/**
 * @author jdcasey
 */
public class DefaultModelInheritanceAssemblerTest
    extends TestCase
{
    private ModelInheritanceAssembler assembler = new DefaultModelInheritanceAssembler();

    public void testShouldOverrideUnitTestExcludesOnly()
    {
        Model parent = new Model();

        parent.setGroupId( "test" );
        parent.setArtifactId( "test" );
        parent.setVersion( "0.0" );

        Build parentBuild = new Build();

        parentBuild.setSourceDirectory( "src/main/java" );
        parentBuild.setTestSourceDirectory( "src/test/java" );

        Resource parentResource = new Resource();

        parentResource.setDirectory( "src/main/resources" );

        parentBuild.addResource( parentResource );

        Resource parentUTResource = new Resource();

        parentUTResource.setDirectory( "src/test/resources" );

        parentBuild.addTestResource( parentUTResource );

        parent.setBuild( parentBuild );

        Model child = new Model();

        Parent parentElement = new Parent();
        parentElement.setArtifactId( parent.getArtifactId() );
        parentElement.setGroupId( parent.getGroupId() );
        parentElement.setVersion( parent.getVersion() );
        child.setParent( parentElement );

        child.setPackaging( "plugin" );

        Build childBuild = new Build();
        child.setBuild( childBuild );

        assembler.assembleModelInheritance( child, parent );

        assertEquals( "source directory should be from parent", "src/main/java",
                      child.getBuild().getSourceDirectory() );
        assertEquals( "unit test source directory should be from parent", "src/test/java",
                      child.getBuild().getTestSourceDirectory() );

// TODO: test inheritence/super pom?
//        List childExcludesTest = child.getBuild().getUnitTest().getExcludes();
//
//        assertTrue( "unit test excludes should have **/*AspectTest.java", childExcludesTest
//            .contains( "**/*AspectTest.java" ) );
//        assertTrue( "unit test excludes should have **/*Abstract*.java", childExcludesTest
//            .contains( "**/*Abstract*.java" ) );
//

        List resources = child.getBuild().getResources();

        assertEquals( "build resources inherited from parent should be of size 1", 1, resources.size() );
        assertEquals( "first resource should have dir == src/main/resources", "src/main/resources",
                      ( (Resource) resources.get( 0 ) ).getDirectory() );

        List utResources = child.getBuild().getTestResources();

        assertEquals( "UT resources inherited from parent should be of size 1", 1, utResources.size() );
        assertEquals( "first UT resource should have dir == src/test/resources", "src/test/resources",
                      ( (Resource) utResources.get( 0 ) ).getDirectory() );

        assertEquals( "plugin", child.getPackaging() );
        assertEquals( "jar", parent.getPackaging() );
    }

    /**
     * <pre>
     * root
     *   |--artifact1
     *   |         |
     *   |         |--artifact1-1
     *   |
     *   |--artifact2 (in another directory called a2 so it has it's own scm section)
     *             |
     *             |--artifact2-1
     * </pre>
     */
    public void testScmInheritance()
        throws Exception
    {
        // Make the models
        Model root = makeScmModel( "root", "scm:foo:/scm-root", "scm:foo:/scm-dev-root", null );

        Model artifact1 = makeScmModel( "artifact1" );

        Model artifact1_1 = makeScmModel( "artifact1-1" );

        Model artifact2 = makeScmModel( "artifact2", "scm:foo:/scm-root/yay-artifact2",
                                        "scm:foo:/scm-dev-root/yay-artifact2", null );

        Model artifact2_1 = makeScmModel( "artifact2-1" );

        // Assemble
        assembler.assembleModelInheritance( artifact1, root );

        assembler.assembleModelInheritance( artifact1_1, artifact1 );

        assembler.assembleModelInheritance( artifact2, root );

        assembler.assembleModelInheritance( artifact2_1, artifact2 );

        // --- -- -

        assertConnection( "scm:foo:/scm-root/artifact1", "scm:foo:/scm-dev-root/artifact1", artifact1 );

        assertConnection( "scm:foo:/scm-root/artifact1/artifact1-1", "scm:foo:/scm-dev-root/artifact1/artifact1-1",
                          artifact1_1 );

        assertConnection( "scm:foo:/scm-root/yay-artifact2", "scm:foo:/scm-dev-root/yay-artifact2", artifact2 );

        assertConnection( "scm:foo:/scm-root/yay-artifact2/artifact2-1",
                          "scm:foo:/scm-dev-root/yay-artifact2/artifact2-1", artifact2_1 );
    }

    public void testScmInheritanceWhereParentHasConnectionAndTheChildDoesnt()
    {
        Model parent = makeScmModel( "parent", "scm:foo:bar:/scm-root", null, null );

        Model child = makeScmModel( "child" );

        assembler.assembleModelInheritance( child, parent );

        assertScm( "scm:foo:bar:/scm-root/child", null, null, child.getScm() );
    }

    public void testScmInheritanceWhereParentHasConnectionAndTheChildDoes()
    {
        Model parent = makeScmModel( "parent", "scm:foo:bar:/scm-root", null, null );

        Model child = makeScmModel( "child", "scm:foo:bar:/another-root", null, null );

        assembler.assembleModelInheritance( child, parent );

        assertScm( "scm:foo:bar:/another-root", null, null, child.getScm() );
    }

    public void testScmInheritanceWhereParentHasDeveloperConnectionAndTheChildDoesnt()
    {
        Model parent = makeScmModel( "parent", null, "scm:foo:bar:/scm-dev-root", null );

        Model child = makeScmModel( "child" );

        assembler.assembleModelInheritance( child, parent );

        assertScm( null, "scm:foo:bar:/scm-dev-root/child", null, child.getScm() );
    }

    public void testScmInheritanceWhereParentHasDeveloperConnectionAndTheChildDoes()
    {
        Model parent = makeScmModel( "parent", null, "scm:foo:bar:/scm-dev-root", null );

        Model child = makeScmModel( "child", null, "scm:foo:bar:/another-dev-root", null );

        assembler.assembleModelInheritance( child, parent );

        assertScm( null, "scm:foo:bar:/another-dev-root", null, child.getScm() );
    }

    public void testScmInheritanceWhereParentHasUrlAndTheChildDoesnt()
    {
        Model parent = makeScmModel( "parent", null, null, "http://foo/bar" );

        Model child = makeScmModel( "child" );

        assembler.assembleModelInheritance( child, parent );

        assertScm( null, null, "http://foo/bar/child", child.getScm() );
    }

    public void testScmInheritanceWhereParentHasUrlAndTheChildDoes()
    {
        Model parent = makeScmModel( "parent", null, null, "http://foo/bar/" );

        Model child = makeScmModel( "child", null, null, "http://bar/foo/" );

        assembler.assembleModelInheritance( child, parent );

        assertScm( null, null, "http://bar/foo/", child.getScm() );
    }

    public void testRepositoryInheritenceWhereParentHasRepositoryAndTheChildDoesnt()
    {
        Model parent = makeRepositoryModel( "parent", "central", "http://repo1.maven.org/maven/" );

        List repos = new ArrayList( parent.getRepositories() );

        Model child = makeBaseModel( "child" );

        assembler.assembleModelInheritance( child, parent );

        // TODO: a lot easier if modello generated equals() :)
        assertRepositories( repos, child.getRepositories() );
    }

    public void testRepositoryInheritenceWhereParentHasRepositoryAndTheChildHasDifferent()
    {
        Model parent = makeRepositoryModel( "parent", "central", "http://repo1.maven.org/maven/" );

        List repos = new ArrayList( parent.getRepositories() );

        Model child = makeRepositoryModel( "child", "workplace", "http://repository.mycompany.com/maven/" );

        repos.addAll( child.getRepositories() );

        assembler.assembleModelInheritance( child, parent );

        // TODO: a lot easier if modello generated equals() :)
        assertRepositories( repos, child.getRepositories() );
    }

    public void testRepositoryInheritenceWhereParentHasRepositoryAndTheChildHasSameId()
    {
        Model parent = makeRepositoryModel( "parent", "central", "http://repo1.maven.org/maven/" );

        Model child = makeRepositoryModel( "child", "central", "http://repo2.maven.org/maven/" );

        // We want to get the child repository here.
        List repos = new ArrayList( child.getRepositories() );

        assembler.assembleModelInheritance( child, parent );

        // TODO: a lot easier if modello generated equals() :)
        assertRepositories( repos, child.getRepositories() );
    }

    public void testPluginInheritanceWhereParentPluginWithoutInheritFlagAndChildHasNoPlugins()
    {
        Model parent = makeBaseModel( "parent" );

        Model child = makeBaseModel( "child" );

        Plugin parentPlugin = new Plugin();
        parentPlugin.setArtifactId( "maven-testInheritance-plugin" );
        parentPlugin.setGroupId( "org.apache.maven.plugins" );
        parentPlugin.setVersion( "1.0" );

        List parentPlugins = Collections.singletonList( parentPlugin );

        Build parentBuild = new Build();
        parentBuild.setPlugins( parentPlugins );

        parent.setBuild( parentBuild );

        assembler.assembleModelInheritance( child, parent );

        assertPlugins( parentPlugins, child );
    }

    public void testPluginInheritanceWhereParentPluginWithTrueInheritFlagAndChildHasNoPlugins()
    {
        Model parent = makeBaseModel( "parent" );

        Model child = makeBaseModel( "child" );

        Plugin parentPlugin = new Plugin();
        parentPlugin.setArtifactId( "maven-testInheritance2-plugin" );
        parentPlugin.setGroupId( "org.apache.maven.plugins" );
        parentPlugin.setVersion( "1.0" );
        parentPlugin.setInherited( "true" );

        List parentPlugins = Collections.singletonList( parentPlugin );

        Build parentBuild = new Build();
        parentBuild.setPlugins( parentPlugins );

        parent.setBuild( parentBuild );

        assembler.assembleModelInheritance( child, parent );

        assertPlugins( parentPlugins, child );
    }

    public void testPluginInheritanceWhereParentPluginWithFalseInheritFlagAndChildHasNoPlugins()
    {
        Model parent = makeBaseModel( "parent" );

        Model child = makeBaseModel( "child" );

        Plugin parentPlugin = new Plugin();
        parentPlugin.setArtifactId( "maven-testInheritance3-plugin" );
        parentPlugin.setGroupId( "org.apache.maven.plugins" );
        parentPlugin.setVersion( "1.0" );
        parentPlugin.setInherited( "false" );

        List parentPlugins = Collections.singletonList( parentPlugin );

        Build parentBuild = new Build();
        parentBuild.setPlugins( parentPlugins );

        parent.setBuild( parentBuild );

        assembler.assembleModelInheritance( child, parent );

        assertPlugins( new ArrayList(), child );
    }

    private void assertPlugins( List expectedPlugins, Model child )
    {
        Build childBuild = child.getBuild();

        if ( expectedPlugins != null && !expectedPlugins.isEmpty() )
        {
            assertNotNull( childBuild );

            Map childPluginsMap = childBuild.getPluginsAsMap();

            if ( childPluginsMap != null )
            {
                assertEquals( expectedPlugins.size(), childPluginsMap.size() );

                for ( Iterator it = expectedPlugins.iterator(); it.hasNext(); )
                {
                    Plugin expectedPlugin = (Plugin) it.next();

                    Plugin childPlugin = (Plugin) childPluginsMap.get( expectedPlugin.getKey() );

                    assertPluginsEqual( expectedPlugin, childPlugin );
                }
            }
            else
            {
                fail( "child plugins collection is null, but expectations map is not." );
            }
        }
        else
        {
            assertTrue( childBuild == null || childBuild.getPlugins() == null || childBuild.getPlugins().isEmpty() );
        }
    }

    private void assertPluginsEqual( Plugin reference, Plugin test )
    {
        assertEquals( "Plugin keys don't match", reference.getKey(), test.getKey() );
        assertEquals( "Plugin configurations don't match", reference.getConfiguration(), test.getConfiguration() );

        List referenceGoals = reference.getGoals();
        Map testGoalsMap = test.getGoalsAsMap();

        if ( referenceGoals != null && !referenceGoals.isEmpty() )
        {
            assertTrue( "Missing goals specification", ( testGoalsMap != null && !testGoalsMap.isEmpty() ) );

            for ( Iterator it = referenceGoals.iterator(); it.hasNext(); )
            {
                Goal referenceGoal = (Goal) it.next();
                Goal testGoal = (Goal) testGoalsMap.get( referenceGoal.getId() );

                assertNotNull( "Goal from reference not found in test", testGoal );

                assertEquals( "Goal IDs don't match", referenceGoal.getId(), testGoal.getId() );
                assertEquals( "Goal configurations don't match", referenceGoal.getConfiguration(),
                              testGoal.getConfiguration() );
            }
        }
        else
        {
            assertTrue( "Unexpected goals specification", ( testGoalsMap == null || testGoalsMap.isEmpty() ) );
        }
    }

    // ----------------------------------------------------------------------
    //
    // ----------------------------------------------------------------------

    private void assertConnection( String expectedConnection, String expectedDeveloperConnection, Model model )
    {
        String connection = model.getScm().getConnection();

        assertNotNull( connection );

        assertEquals( expectedConnection, connection );

        String developerConnection = model.getScm().getDeveloperConnection();

        assertNotNull( developerConnection );

        assertEquals( expectedDeveloperConnection, developerConnection );
    }

    public void assertScm( String connection, String developerConnection, String url, Scm scm )
    {
        assertNotNull( scm );

        assertEquals( connection, scm.getConnection() );

        assertEquals( developerConnection, scm.getDeveloperConnection() );

        assertEquals( url, scm.getUrl() );
    }

    private static Model makeScmModel( String artifactId )
    {
        return makeScmModel( artifactId, null, null, null );
    }

    private static Model makeScmModel( String artifactId, String connection, String developerConnection, String url )
    {
        Model model = makeBaseModel( artifactId );

        if ( connection != null || developerConnection != null || url != null )
        {
            Scm scm = new Scm();

            scm.setConnection( connection );

            scm.setDeveloperConnection( developerConnection );

            scm.setUrl( url );

            model.setScm( scm );
        }

        return model;
    }

    private static Model makeBaseModel( String artifactId )
    {
        Model model = new Model();

        model.setModelVersion( "4.0.0" );

        model.setGroupId( "maven" );

        model.setArtifactId( artifactId );

        model.setVersion( "1.0" );
        return model;
    }

    private static Model makeRepositoryModel( String artifactId, String id, String url )
    {
        Model model = makeBaseModel( artifactId );

        Repository repository = new Repository();
        repository.setId( id );
        repository.setUrl( url );

        model.setRepositories( new ArrayList( Collections.singletonList( repository ) ) );

        return model;
    }

    private void assertRepositories( List expected, List actual )
    {
        assertEquals( "Repository list sizes don't match", expected.size(), actual.size() );

        for ( Iterator i = expected.iterator(); i.hasNext(); )
        {
            Repository expectedRepository = (Repository) i.next();
            boolean found = false;
            for ( Iterator j = actual.iterator(); j.hasNext() && !found; )
            {
                Repository actualRepository = (Repository) j.next();

                if ( actualRepository.getId().equals( expectedRepository.getId() ) )
                {
                    assertEquals( "Repository URLs don't match", expectedRepository.getUrl(),
                                  actualRepository.getUrl() );
                    found = true;
                }
            }
            assertTrue( "Repository with ID " + expectedRepository.getId() + " not found", found );
        }
    }

}
