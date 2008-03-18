package org.apache.maven.project.inheritance;

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

import org.apache.maven.model.Build;
import org.apache.maven.model.Dependency;
import org.apache.maven.model.DependencyManagement;
import org.apache.maven.model.DeploymentRepository;
import org.apache.maven.model.DistributionManagement;
import org.apache.maven.model.Model;
import org.apache.maven.model.Parent;
import org.apache.maven.model.Plugin;
import org.apache.maven.model.PluginExecution;
import org.apache.maven.model.Relocation;
import org.apache.maven.model.ReportPlugin;
import org.apache.maven.model.ReportSet;
import org.apache.maven.model.Reporting;
import org.apache.maven.model.Repository;
import org.apache.maven.model.RepositoryBase;
import org.apache.maven.model.Resource;
import org.apache.maven.model.Scm;
import org.apache.maven.model.Site;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import junit.framework.TestCase;

/**
 * @author jdcasey
 */
public class DefaultModelInheritanceAssemblerTest
    extends TestCase
{
    private ModelInheritanceAssembler assembler = new DefaultModelInheritanceAssembler();

    public void testShouldAdjustChildUrlBasedOnParentAndModulePathInSiblingDir()
    {
        Model parent = makeBaseModel( "parent" );

        parent.setUrl( "http://www.google.com/parent" );

        Model child = makeBaseModel( "child" );

        // TODO: this is probably what we should be appending...
//        child.setUrl( "/child.dir" );

        parent.addModule( "../child" );

        assembler.assembleModelInheritance( child, parent, ".." );

        String resultingUrl = child.getUrl();

        System.out.println( resultingUrl );

        assertEquals( "http://www.google.com/child", resultingUrl );
    }

    public void testShouldAdjustPathsThreeLevelsDeepAncestryInRepoAndNonStandardModulePaths()
    {
        Model top = makeBaseModel( "top" );

        top.setUrl( "http://www.google.com/top" );

        Model middle = makeBaseModel( "middle" );

        top.addModule( "../middle" );

        Model bottom = makeBaseModel( "bottom" );

        middle.addModule( "../bottom" );

        assembler.assembleModelInheritance( middle, top, ".." );
        assembler.assembleModelInheritance( bottom, middle, ".." );

        String resultingUrl = bottom.getUrl();

        System.out.println( resultingUrl );

        assertEquals( "http://www.google.com/bottom", resultingUrl );
    }

    public void testShouldMergeSuccessiveDependencyManagementSectionsOverThreeLevels()
    {
        Model top = makeBaseModel( "top" );

        DependencyManagement topMgmt = new DependencyManagement();

        topMgmt.addDependency( makeDep( "top-dep" ) );

        top.setDependencyManagement( topMgmt );

        Model mid = makeBaseModel( "mid" );

        DependencyManagement midMgmt = new DependencyManagement();

        midMgmt.addDependency( makeDep( "mid-dep" ) );

        mid.setDependencyManagement( midMgmt );

        Model bottom = makeBaseModel( "bottom" );

        DependencyManagement bottomMgmt = new DependencyManagement();

        bottomMgmt.addDependency( makeDep( "bottom-dep" ) );

        bottom.setDependencyManagement( bottomMgmt );

        assembler.assembleModelInheritance( mid, top );

        assembler.assembleModelInheritance( bottom, mid );

        DependencyManagement result = bottom.getDependencyManagement();

        List resultDeps = result.getDependencies();

        assertEquals( 3, resultDeps.size() );
    }

    public void testShouldMergeDependencyManagementSectionsFromTopTwoLevelsToBottomLevel()
    {
        Model top = makeBaseModel( "top" );

        DependencyManagement topMgmt = new DependencyManagement();

        topMgmt.addDependency( makeDep( "top-dep" ) );

        top.setDependencyManagement( topMgmt );

        Model mid = makeBaseModel( "mid" );

        DependencyManagement midMgmt = new DependencyManagement();

        midMgmt.addDependency( makeDep( "mid-dep" ) );

        mid.setDependencyManagement( midMgmt );

        Model bottom = makeBaseModel( "bottom" );

        assembler.assembleModelInheritance( mid, top );

        assembler.assembleModelInheritance( bottom, mid );

        DependencyManagement result = bottom.getDependencyManagement();

        List resultDeps = result.getDependencies();

        assertEquals( 2, resultDeps.size() );
    }

    private Dependency makeDep( String artifactId )
    {
        Dependency dep = new Dependency();

        dep.setGroupId( "maven" );
        dep.setArtifactId( artifactId );
        dep.setVersion( "1.0" );

        return dep;
    }

    public void testShouldAppendChildPathAdjustmentWithNoChildPartAndNoParentPart()
    {
        String parentPath = "";
        String childPath = null;
        String pathAdjustment = "../file-management";

        String result =
            ( (DefaultModelInheritanceAssembler) assembler ).appendPath( parentPath, childPath, pathAdjustment, true );

        System.out.println( "Resulting path is: \'" + result + "\'" );

        assertEquals( "Append with path adjustment failed.", "/file-management", result );
    }

    public void testShouldAppendChildPathAdjustmentWithNoChildPart()
    {
        String parentPath = "http://maven.apache.org/shared/maven-shared-parent";
        String childPath = null;
        String pathAdjustment = "../file-management";

        String result =
            ( (DefaultModelInheritanceAssembler) assembler ).appendPath( parentPath, childPath, pathAdjustment, true );

        System.out.println( "Resulting path is: \'" + result + "\'" );

        assertEquals( "Append with path adjustment failed.", "http://maven.apache.org/shared/file-management", result );
    }

    public void testShouldAppendPathWithChildPathAdjustment()
    {
        String parentPath = "http://maven.apache.org/shared/maven-shared-parent";
        String childPath = "file-management";
        String pathAdjustment = "..";

        String result =
            ( (DefaultModelInheritanceAssembler) assembler ).appendPath( parentPath, childPath, pathAdjustment, true );

        System.out.println( "Resulting path is: \'" + result + "\'" );

        assertEquals( "Append with path adjustment failed.", "http://maven.apache.org/shared/file-management", result );
    }

    public void testDistributionManagementInheritance()
    {
        Model parent = makeBaseModel( "parent" );
        Model child = makeBaseModel( "child" );

        DistributionManagement distributionManagement = new DistributionManagement();
        distributionManagement.setDownloadUrl( "downloadUrl" );
        distributionManagement.setRelocation( new Relocation() );
        distributionManagement.setStatus( "deployed" );

        DeploymentRepository repository = new DeploymentRepository();
        repository.setId( "apache.releases" );
        repository.setUrl( "scp://minotaur.apache.org/www/www.apache.org/dist/java-repository" );
        repository.setName( "name" );
        repository.setLayout( "legacy" );
        distributionManagement.setRepository( repository );

        DeploymentRepository snapshotRepository = new DeploymentRepository();
        snapshotRepository.setId( "apache.snapshots" );
        snapshotRepository.setUrl( "scp://minotaur.apache.org/www/cvs.apache.org/repository" );
        snapshotRepository.setName( "name" );
        snapshotRepository.setLayout( "legacy" );
        snapshotRepository.setUniqueVersion( false );
        distributionManagement.setSnapshotRepository( snapshotRepository );

        Site site = new Site();
        site.setId( "apache.website" );
        site.setUrl( "scp://minotaur.apache.org/www/maven.apache.org/" );
        site.setName( "name3" );
        distributionManagement.setSite( site );

        parent.setDistributionManagement( distributionManagement );

        assembler.assembleModelInheritance( child, parent );

        DistributionManagement childDistMgmt = child.getDistributionManagement();
        assertNotNull( "Check distMgmt inherited", childDistMgmt );
        assertNull( "Check status NOT inherited", childDistMgmt.getStatus() );
        assertNull( "Check relocation NOT inherited", childDistMgmt.getRelocation() );
        assertEquals( "Check downloadUrl inherited", distributionManagement.getDownloadUrl(),
                      childDistMgmt.getDownloadUrl() );

        Site childSite = childDistMgmt.getSite();
        assertNotNull( "Check site inherited", childSite );
        assertEquals( "Check id matches", site.getId(), childSite.getId() );
        assertEquals( "Check name matches", site.getName(), childSite.getName() );
        assertEquals( "Check url matches with appended path", site.getUrl() + "child", childSite.getUrl() );

        assertRepositoryBase( childDistMgmt.getRepository(), repository );
        assertRepositoryBase( childDistMgmt.getSnapshotRepository(), snapshotRepository );
        assertEquals( "Check uniqueVersion is inherited", snapshotRepository.isUniqueVersion(),
                      childDistMgmt.getSnapshotRepository().isUniqueVersion() );
    }

    public void testThreeLevelDistributionManagementInheritance()
    {
        Model gpar = makeBaseModel( "gpar" );
        Model parent = makeBaseModel( "parent" );
        Model child = makeBaseModel( "child" );

        DistributionManagement distributionManagement = new DistributionManagement();
        distributionManagement.setDownloadUrl( "downloadUrl" );
        distributionManagement.setRelocation( new Relocation() );
        distributionManagement.setStatus( "deployed" );

        DeploymentRepository repository = new DeploymentRepository();
        repository.setId( "apache.releases" );
        repository.setUrl( "scp://minotaur.apache.org/www/www.apache.org/dist/java-repository" );
        repository.setName( "name" );
        repository.setLayout( "legacy" );
        distributionManagement.setRepository( repository );

        DeploymentRepository snapshotRepository = new DeploymentRepository();
        snapshotRepository.setId( "apache.snapshots" );
        snapshotRepository.setUrl( "scp://minotaur.apache.org/www/cvs.apache.org/repository" );
        snapshotRepository.setName( "name" );
        snapshotRepository.setLayout( "legacy" );
        snapshotRepository.setUniqueVersion( false );
        distributionManagement.setSnapshotRepository( snapshotRepository );

        Site site = new Site();
        site.setId( "apache.website" );
        site.setUrl( "scp://minotaur.apache.org/www/maven.apache.org/" );
        site.setName( "name3" );
        distributionManagement.setSite( site );

        gpar.setDistributionManagement( distributionManagement );

        assembler.assembleModelInheritance( parent, gpar );
        assembler.assembleModelInheritance( child, parent );

        DistributionManagement childDistMgmt = child.getDistributionManagement();
        assertNotNull( "Check distMgmt inherited", childDistMgmt );
        assertNull( "Check status NOT inherited", childDistMgmt.getStatus() );
        assertNull( "Check relocation NOT inherited", childDistMgmt.getRelocation() );
        assertEquals( "Check downloadUrl inherited", distributionManagement.getDownloadUrl(),
                      childDistMgmt.getDownloadUrl() );

        Site childSite = childDistMgmt.getSite();
        assertNotNull( "Check site inherited", childSite );
        assertEquals( "Check id matches", site.getId(), childSite.getId() );
        assertEquals( "Check name matches", site.getName(), childSite.getName() );
        assertEquals( "Check url matches with appended path", site.getUrl() + "parent/child", childSite.getUrl() );

        assertRepositoryBase( childDistMgmt.getRepository(), repository );
        assertRepositoryBase( childDistMgmt.getSnapshotRepository(), snapshotRepository );
        assertEquals( "Check uniqueVersion is inherited", snapshotRepository.isUniqueVersion(),
                      childDistMgmt.getSnapshotRepository().isUniqueVersion() );
    }

    private static void assertRepositoryBase( RepositoryBase childRepository, RepositoryBase repository )
    {
        assertNotNull( "Check repository inherited", childRepository );
        assertEquals( "Check id matches", repository.getId(), childRepository.getId() );
        assertEquals( "Check name matches", repository.getName(), childRepository.getName() );
        assertEquals( "Check url matches", repository.getUrl(), childRepository.getUrl() );
        assertEquals( "Check layout matches", repository.getLayout(), childRepository.getLayout() );
    }

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

        Model artifact2 =
            makeScmModel( "artifact2", "scm:foo:/scm-root/yay-artifact2", "scm:foo:/scm-dev-root/yay-artifact2", null );

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

        if ( ( expectedPlugins != null ) && !expectedPlugins.isEmpty() )
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
            assertTrue( ( childBuild == null ) || ( childBuild.getPlugins() == null ) || childBuild.getPlugins().isEmpty() );
        }
    }

    private void assertPluginsEqual( Plugin reference, Plugin test )
    {
        assertEquals( "Plugin keys don't match", reference.getKey(), test.getKey() );
        assertEquals( "Plugin configurations don't match", reference.getConfiguration(), test.getConfiguration() );

        List referenceExecutions = reference.getExecutions();
        Map testExecutionsMap = test.getExecutionsAsMap();

        if ( ( referenceExecutions != null ) && !referenceExecutions.isEmpty() )
        {
            assertTrue( "Missing goals specification", ( ( testExecutionsMap != null ) && !testExecutionsMap.isEmpty() ) );

            for ( Iterator it = referenceExecutions.iterator(); it.hasNext(); )
            {
                PluginExecution referenceExecution = (PluginExecution) it.next();
                PluginExecution testExecution = (PluginExecution) testExecutionsMap.get( referenceExecution.getId() );

                assertNotNull( "Goal from reference not found in test", testExecution );

                assertEquals( "Goal IDs don't match", referenceExecution.getId(), testExecution.getId() );
                assertEquals( "Goal configurations don't match", referenceExecution.getConfiguration(),
                              testExecution.getConfiguration() );
                assertEquals( "Goal lists don't match", referenceExecution.getGoals(), testExecution.getGoals() );
            }
        }
        else
        {
            assertTrue( "Unexpected goals specification",
                        ( ( testExecutionsMap == null ) || testExecutionsMap.isEmpty() ) );
        }
    }

    public void testReportingExcludeDefaultsInheritance()
    {
        Model parent = makeBaseModel( "parent" );

        Model child = makeBaseModel( "child" );

        Reporting parentBuild = new Reporting();
        parentBuild.setExcludeDefaults( false );
        parent.setReporting( parentBuild );

        assembler.assembleModelInheritance( child, parent );

        assertFalse( "Check excludeDefaults is inherited", child.getReporting().isExcludeDefaults() );

        child = makeBaseModel( "child" );

        parentBuild.setExcludeDefaults( true );

        assembler.assembleModelInheritance( child, parent );

        assertTrue( "Check excludeDefaults is inherited", child.getReporting().isExcludeDefaults() );
    }

    public void testReportInheritanceWhereParentReportWithoutInheritFlagAndChildHasNoReports()
    {
        Model parent = makeBaseModel( "parent" );

        Model child = makeBaseModel( "child" );

        ReportPlugin parentReport = new ReportPlugin();
        parentReport.setArtifactId( "maven-testInheritance-report-plugin" );
        parentReport.setGroupId( "org.apache.maven.plugins" );
        parentReport.setVersion( "1.0" );

        List parentPlugins = Collections.singletonList( parentReport );

        Reporting parentBuild = new Reporting();
        parentBuild.setPlugins( parentPlugins );

        parent.setReporting( parentBuild );

        assembler.assembleModelInheritance( child, parent );

        assertReports( parentPlugins, child );
    }

    public void testReportInheritanceWhereParentReportWithTrueInheritFlagAndChildHasNoReports()
    {
        Model parent = makeBaseModel( "parent" );

        Model child = makeBaseModel( "child" );

        ReportPlugin parentPlugin = new ReportPlugin();
        parentPlugin.setArtifactId( "maven-testInheritance2-report-plugin" );
        parentPlugin.setGroupId( "org.apache.maven.plugins" );
        parentPlugin.setVersion( "1.0" );
        parentPlugin.setInherited( "true" );

        List parentPlugins = Collections.singletonList( parentPlugin );

        Reporting parentBuild = new Reporting();
        parentBuild.setPlugins( parentPlugins );

        parent.setReporting( parentBuild );

        assembler.assembleModelInheritance( child, parent );

        assertReports( parentPlugins, child );
    }

    public void testReportInheritanceWhereParentReportWithFalseInheritFlagAndChildHasNoReports()
    {
        Model parent = makeBaseModel( "parent" );

        Model child = makeBaseModel( "child" );

        ReportPlugin parentPlugin = new ReportPlugin();
        parentPlugin.setArtifactId( "maven-testInheritance3-report-plugin" );
        parentPlugin.setGroupId( "org.apache.maven.plugins" );
        parentPlugin.setVersion( "1.0" );
        parentPlugin.setInherited( "false" );

        List parentPlugins = Collections.singletonList( parentPlugin );

        Reporting parentBuild = new Reporting();
        parentBuild.setPlugins( parentPlugins );

        parent.setReporting( parentBuild );

        assembler.assembleModelInheritance( child, parent );

        assertReports( new ArrayList(), child );
    }

    public void testPluginExecInheritanceWhereExecInheritedSetToFalse()
    {
        String testId = "test";
        String gid = "group";
        String aid = "artifact";
        String ver = "1";

        Model child = makeBaseModel( "child" );

        Plugin pChild = new Plugin();
        pChild.setGroupId( gid );
        pChild.setArtifactId( aid );
        pChild.setVersion( ver );

        PluginExecution eChild = new PluginExecution();
        eChild.setId( "normal" );
        eChild.addGoal( "run" );

        pChild.addExecution( eChild );

        Build bChild = new Build();
        bChild.addPlugin( pChild );

        child.setBuild( bChild );

        Model parent = makeBaseModel( "parent" );

        Plugin pParent = new Plugin();
        pParent.setGroupId( gid );
        pParent.setArtifactId( aid );
        pParent.setVersion( ver );

        pParent.setInherited( Boolean.toString( true ) );

        PluginExecution eParent = new PluginExecution();
        eParent.setId( testId );
        eParent.addGoal( "test" );
        eParent.setInherited( Boolean.toString( false ) );

        pParent.addExecution( eParent );

        Build bParent = new Build();
        bParent.addPlugin( pParent );

        parent.setBuild( bParent );

        assembler.assembleModelInheritance( child, parent );

        Map pluginMap = bChild.getPluginsAsMap();
        assertNotNull( pluginMap );

        Plugin plugin = (Plugin) pluginMap.get( gid + ":" + aid );
        assertNotNull( plugin );

        Map executionMap = plugin.getExecutionsAsMap();
        assertNotNull( executionMap );

        assertNull( "test execution with inherited == false should NOT be inherited to child model.", executionMap.get( testId ) );
    }

    public void testPluginExecInheritanceWhereExecInheritedSetToFalseAndPluginInheritedNotSet()
    {
        String testId = "test";
        String gid = "group";
        String aid = "artifact";
        String ver = "1";

        Model child = makeBaseModel( "child" );

        Plugin pChild = new Plugin();
        pChild.setGroupId( gid );
        pChild.setArtifactId( aid );
        pChild.setVersion( ver );

        PluginExecution eChild = new PluginExecution();
        eChild.setId( "normal" );
        eChild.addGoal( "run" );

        pChild.addExecution( eChild );

        Build bChild = new Build();
        bChild.addPlugin( pChild );

        child.setBuild( bChild );

        Model parent = makeBaseModel( "parent" );

        Plugin pParent = new Plugin();
        pParent.setGroupId( gid );
        pParent.setArtifactId( aid );
        pParent.setVersion( ver );

        PluginExecution eParent = new PluginExecution();
        eParent.setId( testId );
        eParent.addGoal( "test" );
        eParent.setInherited( Boolean.toString( false ) );

        pParent.addExecution( eParent );

        Build bParent = new Build();
        bParent.addPlugin( pParent );

        parent.setBuild( bParent );

        assembler.assembleModelInheritance( child, parent );

        Map pluginMap = bChild.getPluginsAsMap();
        assertNotNull( pluginMap );

        Plugin plugin = (Plugin) pluginMap.get( gid + ":" + aid );
        assertNotNull( plugin );

        Map executionMap = plugin.getExecutionsAsMap();
        assertNotNull( executionMap );

        assertNull( "test execution with inherited == false should NOT be inherited to child model.", executionMap.get( testId ) );
    }

    private void assertReports( List expectedPlugins, Model child )
    {
        Reporting childBuild = child.getReporting();

        if ( ( expectedPlugins != null ) && !expectedPlugins.isEmpty() )
        {
            assertNotNull( childBuild );

            Map childPluginsMap = childBuild.getReportPluginsAsMap();

            if ( childPluginsMap != null )
            {
                assertEquals( expectedPlugins.size(), childPluginsMap.size() );

                for ( Iterator it = expectedPlugins.iterator(); it.hasNext(); )
                {
                    ReportPlugin expectedPlugin = (ReportPlugin) it.next();

                    ReportPlugin childPlugin = (ReportPlugin) childPluginsMap.get( expectedPlugin.getKey() );

                    assertReportsEqual( expectedPlugin, childPlugin );
                }
            }
            else
            {
                fail( "child plugins collection is null, but expectations map is not." );
            }
        }
        else
        {
            assertTrue( ( childBuild == null ) || ( childBuild.getPlugins() == null ) || childBuild.getPlugins().isEmpty() );
        }
    }

    private void assertReportsEqual( ReportPlugin reference, ReportPlugin test )
    {
        assertEquals( "Plugin keys don't match", reference.getKey(), test.getKey() );
        assertEquals( "Plugin configurations don't match", reference.getConfiguration(), test.getConfiguration() );

        List referenceReportSets = reference.getReportSets();
        Map testReportSetsMap = test.getReportSetsAsMap();

        if ( ( referenceReportSets != null ) && !referenceReportSets.isEmpty() )
        {
            assertTrue( "Missing goals specification", ( ( testReportSetsMap != null ) && !testReportSetsMap.isEmpty() ) );

            for ( Iterator it = referenceReportSets.iterator(); it.hasNext(); )
            {
                ReportSet referenceReportSet = (ReportSet) it.next();
                ReportSet testReportSet = (ReportSet) testReportSetsMap.get( referenceReportSet.getId() );

                assertNotNull( "Goal from reference not found in test", testReportSet );

                assertEquals( "Goal IDs don't match", referenceReportSet.getId(), testReportSet.getId() );
                assertEquals( "Goal configurations don't match", referenceReportSet.getConfiguration(),
                              testReportSet.getConfiguration() );
                assertEquals( "Reports don't match", referenceReportSet.getReports(), testReportSet.getReports() );
            }
        }
        else
        {
            assertTrue( "Unexpected goals specification",
                        ( ( testReportSetsMap == null ) || testReportSetsMap.isEmpty() ) );
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

        if ( ( connection != null ) || ( developerConnection != null ) || ( url != null ) )
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

        Repository repository = makeRepository( id, url );

        model.setRepositories( new ArrayList( Collections.singletonList( repository ) ) );

        return model;
    }

    private static Repository makeRepository( String id, String url )
    {
        Repository repository = new Repository();
        repository.setId( id );
        repository.setUrl( url );
        return repository;
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
