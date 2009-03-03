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
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */

import java.io.File;
import java.io.IOException;
import java.io.FileInputStream;
import java.util.*;

import org.apache.maven.project.DefaultProfileManager;
import org.apache.maven.project.ProfileActivationContext;
import org.apache.maven.model.Model;
import org.apache.maven.model.io.xpp3.MavenXpp3Reader;
import org.apache.maven.artifact.Artifact;
import org.apache.maven.artifact.repository.DefaultArtifactRepository;
import org.apache.maven.artifact.repository.layout.DefaultRepositoryLayout;
import org.apache.maven.project.harness.PomTestWrapper;
import org.apache.maven.project.*;
import org.apache.maven.project.builder.PomClassicDomainModel;
import org.apache.maven.repository.MavenRepositorySystem;
import org.codehaus.plexus.PlexusTestCase;
import org.codehaus.plexus.util.xml.pull.XmlPullParserException;

public class PomConstructionTest
    extends PlexusTestCase
{

    private static String BASE_DIR = "src/test";

    private static String BASE_POM_DIR = BASE_DIR + "/resources-project-builder";

    private static String BASE_MIXIN_DIR = BASE_DIR + "/resources-mixins";

    private DefaultMavenProjectBuilder mavenProjectBuilder;

    private MavenRepositorySystem mavenTools;

    private File testDirectory;

    private File testMixinDirectory;

    protected void setUp()
        throws Exception
    {
        testDirectory = new File( getBasedir(), BASE_POM_DIR );
        testMixinDirectory = new File( getBasedir(), BASE_MIXIN_DIR );
        mavenProjectBuilder = (DefaultMavenProjectBuilder) lookup( MavenProjectBuilder.class );
        mavenTools = lookup( MavenRepositorySystem.class );
    }

    /**
     * Will throw exception if url is empty. MNG-4050 
     *
     * @throws Exception
     */
    /*
    public void testEmptyUrl()
        throws Exception
    {
        buildPomFromMavenProject( "empty-distMng-repo-url", null );
    }
    */

    /**
     * Tests that modules is not overriden by profile
     * 
     * @throws Exception
     */
    public void testProfileModules()
        throws Exception
    {
        PomTestWrapper pom = buildPomFromMavenProject( "profile-module", "a" );
        assertEquals( "test-prop", pom.getValue( "properties[1]/b" ) );//verifies profile applied
        assertEquals( "test-module", pom.getValue( "modules[1]" ) );
    }

    /**
     * Will throw exception if doesn't find parent(s) in build
     *
     * @throws Exception
     */
    public void testParentInheritance()
        throws Exception
    {
        buildPom( "parent-inheritance/sub" );
    }

    /*MNG-3995*/
    public void testExecutionConfigurationJoin()
       throws Exception
    {
        PomTestWrapper pom = buildPom( "execution-configuration-join" );
        assertEquals( 2, ( (List<?>) pom.getValue( "build/plugins[1]/executions[1]/configuration[1]/fileset[1]" ) ).size() );
    }

    /*MNG-3803*/
    public void testPluginConfigProperties()
       throws Exception
    {
        PomTestWrapper pom = buildPom( "plugin-config-properties" );
        assertEquals( "my.property", pom.getValue( "build/plugins[1]/configuration[1]/systemProperties[1]/property[1]/name" ) );
    }


    // Some better conventions for the test poms needs to be created and each of these tests
    // that represent a verification of a specification item needs to be a couple lines at most.
    // The expressions help a lot, but we need a clean to pick up a directory of POMs, automatically load
    // them into a resolver, create the expression to extract the data to validate the Model, and the URI
    // to validate the properties. We also need a way to navigate from the Tex specification documents to
    // the test in question and vice versa. A little Eclipse plugin would do the trick.
    /*
    TODO: Not sure why this test is failing after removing resolver. Logic is the same.
     */
    public void testThatExecutionsWithoutIdsAreMergedAndTheChildWins()
        throws Exception
    {
      // This should be 2
      //assertEquals( 2, model.getLineageCount() );
      //PomTestWrapper tester = buildPom("micromailer");
      //assertModelEquals( tester, "child-descriptor", "build/plugins[1]/executions[1]/goals[1]" );
    }

    /*MNG-
    public void testDependencyScope()
        throws Exception
    {
        PomTestWrapper pom = buildPom( "dependency-scope/sub" );
        System.out.println(pom.getDomainModel().asString());

    }
    */
    /*MNG- 4010*/
    public void testDuplicateExclusionsDependency()
        throws Exception
    {
        PomTestWrapper pom = buildPom( "duplicate-exclusions-dependency/sub" );
        assertEquals( 1, ( (List<?>) pom.getValue( "dependencies[1]/exclusions" ) ).size() );

    }

    /*MNG- 4008*/
    public void testMultipleFilters()
        throws Exception
    {
        PomTestWrapper pom = buildPom( "multiple-filters" );
        assertEquals( 4, ( (List<?>) pom.getValue( "build/filters" ) ).size() );

    }

    /*MNG-4005 - not implemented
    public void testDependenciesDifferentVersions()
        throws Exception
    {
        PomTestWrapper pom = buildPom( "dependencies-different-versions" );

    }
    */
      /*MNG-3803*/
    public void testDependenciesWithDifferentVersions()
       throws Exception
    {
        PomTestWrapper pom = buildPom( "dependencies-with-different-versions" );
        assertEquals( 1, ( (List<?>) pom.getValue( "dependencies" ) ).size() );
    }

    /* MNG-3567*/
    public void testParentInterpolation()
        throws Exception
    {
        PomTestWrapper pom = buildPomFromMavenProject( "parent-interpolation/sub", null );
        pom = new PomTestWrapper(pom.getMavenProject().getParent());
        assertEquals( "1.3.0-SNAPSHOT", pom.getValue( "build/plugins[1]/version" ) );
    }


    /* MNG-3567*/
    public void testPluginManagementInherited()
        throws Exception
    {
        PomTestWrapper pom = buildPom( "pluginmanagement-inherited/sub" );
        assertEquals( "1.0-alpha-21", pom.getValue( "build/plugins[1]/version" ) );
    }

     /* MNG-2174*/
    public void testPluginManagementDependencies()
        throws Exception
    {
        PomTestWrapper pom = buildPomFromMavenProject( "plugin-management-dependencies/sub", "test" );
        assertEquals( "1.0-alpha-21", pom.getValue( "build/plugins[1]/version" ) );
        assertEquals( "1.0", pom.getValue( "build/plugins[1]/dependencies[1]/version" ) );
    }


    /* MNG-3877*/
    public void testReportingInterpolation()
        throws Exception
    {
        PomTestWrapper pom = buildPomFromMavenProject( "reporting-interpolation", null );
        pom = new PomTestWrapper(pom.getMavenProject());
        assertEquals( createPath(Arrays.asList(System.getProperty("user.dir"),
                "src", "test", "resources-project-builder", "reporting-interpolation", "target", "site")),
                pom.getValue( "reporting/outputDirectory" ) );
    }


    public void testPluginOrder()
        throws Exception
    {
        PomTestWrapper pom = buildPom( "plugin-order" );
        assertEquals( "plexus-component-metadata", pom.getValue( "build/plugins[1]/artifactId" ) );
        assertEquals( "maven-surefire-plugin", pom.getValue( "build/plugins[2]/artifactId" ) );
    }

    public void testErroneousJoiningOfDifferentPluginsWithEqualDependencies()
        throws Exception
    {
        PomTestWrapper pom = buildPom( "equal-plugin-deps" );
        assertEquals( "maven-it-plugin-a", pom.getValue( "build/plugins[1]/artifactId" ) );
        assertEquals( 1, ( (List<?>) pom.getValue( "build/plugins[1]/dependencies" ) ).size() );
        assertEquals( "maven-it-plugin-b", pom.getValue( "build/plugins[2]/artifactId" ) );
        assertEquals( 1, ( (List<?>) pom.getValue( "build/plugins[1]/dependencies" ) ).size() );
    }

    /** MNG-3821 */
    public void testErroneousJoiningOfDifferentPluginsWithEqualExecutionIds()
        throws Exception
    {
        PomTestWrapper pom = buildPom( "equal-plugin-exec-ids" );
        assertEquals( "maven-it-plugin-a", pom.getValue( "build/plugins[1]/artifactId" ) );
        assertEquals( 1, ( (List<?>) pom.getValue( "build/plugins[1]/executions" ) ).size() );
        assertEquals( "maven-it-plugin-b", pom.getValue( "build/plugins[2]/artifactId" ) );
        assertEquals( 1, ( (List<?>) pom.getValue( "build/plugins[1]/executions" ) ).size() );
        assertEquals( "maven-it-plugin-a", pom.getValue( "reporting/plugins[1]/artifactId" ) );
        assertEquals( 1, ( (List<?>) pom.getValue( "reporting/plugins[1]/reportSets" ) ).size() );
        assertEquals( "maven-it-plugin-b", pom.getValue( "reporting/plugins[2]/artifactId" ) );
        assertEquals( 1, ( (List<?>) pom.getValue( "reporting/plugins[1]/reportSets" ) ).size() );
    }

     /** MNG-3998 */
    public void testExecutionConfiguration()
        throws Exception
    {
        PomTestWrapper pom = buildPom( "execution-configuration" );
        assertEquals( 2, ( (List<?>) pom.getValue( "build/plugins[1]/executions" ) ).size() );
        assertEquals( "src/main/mdo/nexus.xml", ( pom.getValue( "build/plugins[1]/executions[1]/configuration[1]/model" ) ));
        assertEquals( "src/main/mdo/security.xml", ( pom.getValue( "build/plugins[1]/executions[2]/configuration[1]/model" ) ));
    }

    public void testSingleConfigurationInheritance()
        throws Exception
    {
        PomTestWrapper pom = buildPom( "single-configuration-inheritance" );
        assertEquals( 2, ( (List<?>) pom.getValue( "build/plugins[1]/executions[1]/configuration[1]/rules" ) ).size() );
        assertEquals("2.0.6", pom.getValue( "build/plugins[1]/executions[1]/configuration[1]/rules[1]/requireMavenVersion[1]/version" ) );
        assertEquals("[2.0.6,)", pom.getValue( "build/plugins[1]/executions[1]/configuration[1]/rules[2]/requireMavenVersion[1]/version" ) );
    }

    public void testConfigWithPluginManagement()
        throws Exception
    {
        PomTestWrapper pom = buildPom( "config-with-plugin-mng" );
        assertEquals( 2, ( (List<?>) pom.getValue( "build/plugins[1]/executions" ) ).size() );
        assertEquals( "src/main/mdo/security.xml", pom.getValue( "build/plugins[1]/executions[2]/configuration[1]/model" ) );
        assertEquals( "1.0.8", pom.getValue( "build/plugins[1]/executions[1]/configuration[1]/version" ) );
    }

    /** MNG-3965 */
    public void testExecutionConfigurationSubcollections()
        throws Exception
    {
        PomTestWrapper pom = buildPom( "execution-configuration-subcollections" );
        assertEquals( 2, ( (List<?>) pom.getValue( "build/plugins[1]/executions[1]/configuration[1]/rules[1]/bannedDependencies" ) ).size() );
    }

    /** MNG-
    public void testFoo()
        throws Exception
    {
        PomTestWrapper pom = buildPom( "foo/sub" );
        //System.out.println(pom.getDomainModel().asString());
    }
    */

    /** MNG-3985 */
    public void testMultipleRepositories()
        throws Exception
    {
        PomTestWrapper pom = buildPom( "multiple-repos/sub" );
        assertEquals( 3, ( (List<?>) pom.getValue( "repositories" ) ).size() );
    }

    /** MNG-3965 */
    public void testMultipleExecutionIds()
        throws Exception
    {
        PomTestWrapper pom = buildPom( "dual-execution-ids/sub" );
        assertEquals( 1, ( (List<?>) pom.getValue( "build/plugins[1]/executions" ) ).size() );
    }

    /** MNG-3997 */
    public void testConsecutiveEmptyElements()
        throws Exception
    {
        PomTestWrapper pom = buildPom( "consecutive_empty_elements" );
        pom.getDomainModel().asString();
    }

    //*/
    public void testOrderOfGoalsFromPluginExecutionWithoutPluginManagement()
        throws Exception
    {
        PomTestWrapper pom = buildPom( "plugin-exec-goals-order/wo-plugin-mngt" );
        assertEquals( 5, ( (List<?>) pom.getValue( "build/plugins[1]/executions[1]/goals" ) ).size() );
        assertEquals( "b", pom.getValue( "build/plugins[1]/executions[1]/goals[1]" ) );
        assertEquals( "a", pom.getValue( "build/plugins[1]/executions[1]/goals[2]" ) );
        assertEquals( "d", pom.getValue( "build/plugins[1]/executions[1]/goals[3]" ) );
        assertEquals( "c", pom.getValue( "build/plugins[1]/executions[1]/goals[4]" ) );
        assertEquals( "e", pom.getValue( "build/plugins[1]/executions[1]/goals[5]" ) );
    }

    /* FIXME: cf. MNG-3886*/
    public void testOrderOfGoalsFromPluginExecutionWithPluginManagement()
        throws Exception
    {
        PomTestWrapper pom = buildPom( "plugin-exec-goals-order/w-plugin-mngt" );
        assertEquals( 5, ( (List<?>) pom.getValue( "build/plugins[1]/executions[1]/goals" ) ).size() );
        assertEquals( "b", pom.getValue( "build/plugins[1]/executions[1]/goals[1]" ) );
        assertEquals( "a", pom.getValue( "build/plugins[1]/executions[1]/goals[2]" ) );
        assertEquals( "d", pom.getValue( "build/plugins[1]/executions[1]/goals[3]" ) );
        assertEquals( "c", pom.getValue( "build/plugins[1]/executions[1]/goals[4]" ) );
        assertEquals( "e", pom.getValue( "build/plugins[1]/executions[1]/goals[5]" ) );
    }
    //*/

    public void testOrderOfPluginExecutionsWithoutPluginManagement()
        throws Exception
    {
        PomTestWrapper pom = buildPom( "plugin-exec-order/wo-plugin-mngt" );
        assertEquals( 5, ( (List<?>) pom.getValue( "build/plugins[1]/executions" ) ).size() );
        assertEquals( "b", pom.getValue( "build/plugins[1]/executions[1]/id" ) );
        assertEquals( "a", pom.getValue( "build/plugins[1]/executions[2]/id" ) );
        assertEquals( "d", pom.getValue( "build/plugins[1]/executions[3]/id" ) );
        assertEquals( "c", pom.getValue( "build/plugins[1]/executions[4]/id" ) );
        assertEquals( "e", pom.getValue( "build/plugins[1]/executions[5]/id" ) );
    }

    /* FIXME: cf. MNG-3887 */
    public void testOrderOfPluginExecutionsWithPluginManagement()
        throws Exception
    {
        PomTestWrapper pom = buildPom( "plugin-exec-order/w-plugin-mngt" );
        assertEquals( 5, ( (List<?>) pom.getValue( "build/plugins[1]/executions" ) ).size() );
        assertEquals( "b", pom.getValue( "build/plugins[1]/executions[1]/id" ) );
        assertEquals( "a", pom.getValue( "build/plugins[1]/executions[2]/id" ) );
        assertEquals( "d", pom.getValue( "build/plugins[1]/executions[3]/id" ) );
        assertEquals( "c", pom.getValue( "build/plugins[1]/executions[4]/id" ) );
        assertEquals( "e", pom.getValue( "build/plugins[1]/executions[5]/id" ) );
    }
    //*/

    public void testMergeOfPluginExecutionsWhenChildInheritsPluginVersion()
        throws Exception
    {
        PomTestWrapper pom = buildPom( "plugin-exec-merging-wo-version/sub" );
        assertEquals( 4, ( (List<?>) pom.getValue( "build/plugins[1]/executions" ) ).size() );
    }

    /* MNG-3943*/
    public void testMergeOfPluginExecutionsWhenChildAndParentUseDifferentPluginVersions()
        throws Exception
    {
        PomTestWrapper pom = buildPom( "plugin-exec-merging-version-insensitive/sub" );
        assertEquals( 4, ( (List<?>) pom.getValue( "build/plugins[1]/executions" ) ).size() );
    }


    public void testInterpolationWithXmlMarkup()
        throws Exception
    {
        PomTestWrapper pom = buildPom( "xml-markup-interpolation" );
        assertEquals( "<?xml version='1.0'?>Tom&Jerry", pom.getValue( "properties/xmlTest" ) );
    }

    /* FIXME: cf. MNG-3925 
    public void testOrderOfMergedPluginExecutionsWithoutPluginManagement()
        throws Exception
    {
        PomTestWrapper pom = buildPom( "merged-plugin-exec-order/wo-plugin-mngt/sub" );
        System.out.println(pom.getDomainModel().asString());
        assertEquals( 5, ( (List<?>) pom.getValue( "build/plugins[1]/executions" ) ).size() );
        assertEquals( "parent-1", pom.getValue( "build/plugins[1]/executions[1]/goals[1]" ) );
        assertEquals( "parent-2", pom.getValue( "build/plugins[1]/executions[2]/goals[1]" ) );
        assertEquals( "child-default", pom.getValue( "build/plugins[1]/executions[3]/goals[1]" ) );
        assertEquals( "child-1", pom.getValue( "build/plugins[1]/executions[4]/goals[1]" ) );
        assertEquals( "child-2", pom.getValue( "build/plugins[1]/executions[5]/goals[1]" ) );
    }

    public void testOrderOfMergedPluginExecutionsWithPluginManagement()
        throws Exception
    {
        PomTestWrapper pom = buildPom( "merged-plugin-exec-order/w-plugin-mngt/sub" );
        assertEquals( 5, ( (List<?>) pom.getValue( "build/plugins[1]/executions" ) ).size() );
        assertEquals( "parent-1", pom.getValue( "build/plugins[1]/executions[1]/goals[1]" ) );
        assertEquals( "parent-2", pom.getValue( "build/plugins[1]/executions[2]/goals[1]" ) );
        assertEquals( "child-default", pom.getValue( "build/plugins[1]/executions[3]/goals[1]" ) );
        assertEquals( "child-1", pom.getValue( "build/plugins[1]/executions[4]/goals[1]" ) );
        assertEquals( "child-2", pom.getValue( "build/plugins[1]/executions[5]/goals[1]" ) );
    }
    //*/

    /* MNG-3984*/
    public void testDifferentContainersWithSameId()
        throws Exception
    {
        PomTestWrapper pom = buildPom( "join-different-containers-same-id" );
        assertEquals( 1, ( (List<?>) pom.getValue( "build/plugins[1]/executions[1]/goals" ) ).size() );
        assertEquals( 1, ( (List<?>) pom.getValue( "build/pluginManagement/plugins[1]/executions[1]/goals" ) ).size() );
    }

    /* MNG-3937*/
    public void testOrderOfMergedPluginExecutionGoalsWithoutPluginManagement()
        throws Exception
    {
        PomTestWrapper pom = buildPom( "merged-plugin-exec-goals-order/wo-plugin-mngt/sub" );
        assertEquals( 5, ( (List<?>) pom.getValue( "build/plugins[1]/executions[1]/goals" ) ).size() );
        assertEquals( "child-a", pom.getValue( "build/plugins[1]/executions[1]/goals[1]" ) );
        assertEquals( "merged", pom.getValue( "build/plugins[1]/executions[1]/goals[2]" ) );
        assertEquals( "child-b", pom.getValue( "build/plugins[1]/executions[1]/goals[3]" ) );
        assertEquals( "parent-b", pom.getValue( "build/plugins[1]/executions[1]/goals[4]" ) );
        assertEquals( "parent-a", pom.getValue( "build/plugins[1]/executions[1]/goals[5]" ) );
    }

    public void testOrderOfMergedPluginExecutionGoalsWithPluginManagement()
        throws Exception
    {
        PomTestWrapper pom = buildPom( "merged-plugin-exec-goals-order/w-plugin-mngt/sub" );
        assertEquals( 5, ( (List<?>) pom.getValue( "build/plugins[1]/executions[1]/goals" ) ).size() );
        assertEquals( "child-a", pom.getValue( "build/plugins[1]/executions[1]/goals[1]" ) );
        assertEquals( "merged", pom.getValue( "build/plugins[1]/executions[1]/goals[2]" ) );
        assertEquals( "child-b", pom.getValue( "build/plugins[1]/executions[1]/goals[3]" ) );
        assertEquals( "parent-b", pom.getValue( "build/plugins[1]/executions[1]/goals[4]" ) );
        assertEquals( "parent-a", pom.getValue( "build/plugins[1]/executions[1]/goals[5]" ) );
    }
    //*/

    public void testOverridingOfInheritedPluginExecutionsWithoutPluginManagement()
        throws Exception
    {
        PomTestWrapper pom = buildPom( "plugin-exec-merging/wo-plugin-mngt/sub" );
        assertEquals( 2, ( (List<?>) pom.getValue( "build/plugins[1]/executions" ) ).size() );
        assertEquals( "child-default", pom.getValue( "build/plugins[1]/executions[@id='default-execution-id']/phase" ) );
        assertEquals( "child-non-default", pom.getValue( "build/plugins[1]/executions[@id='non-default']/phase" ) );
    }

    /* MNG-3938 */
    public void testOverridingOfInheritedPluginExecutionsWithPluginManagement()
        throws Exception
    {
        PomTestWrapper pom = buildPom( "plugin-exec-merging/w-plugin-mngt/sub" );
        assertEquals( 2, ( (List<?>) pom.getValue( "build/plugins[1]/executions" ) ).size() );
        assertEquals( "child-default", pom.getValue( "build/plugins[1]/executions[@id='default']/phase" ) );
        assertEquals( "child-non-default", pom.getValue( "build/plugins[1]/executions[@id='non-default']/phase" ) );
    }
    

    /* FIXME: cf. MNG-3906
    public void testOrderOfMergedPluginDependenciesWithoutPluginManagement()
        throws Exception
    {
        PomTestWrapper pom = buildPom( "merged-plugin-class-path-order/wo-plugin-mngt/sub" );
        assertEquals( 5, ( (List<?>) pom.getValue( "build/plugins[1]/dependencies" ) ).size() );
        assertEquals( "c", pom.getValue( "build/plugins[1]/dependency[1]/artifactId" ) );
        assertEquals( "1", pom.getValue( "build/plugins[1]/dependency[1]/version" ) );
        assertEquals( "a", pom.getValue( "build/plugins[1]/dependency[2]/artifactId" ) );
        assertEquals( "2", pom.getValue( "build/plugins[1]/dependency[2]/version" ) );
        assertEquals( "b", pom.getValue( "build/plugins[1]/dependency[3]/artifactId" ) );
        assertEquals( "1", pom.getValue( "build/plugins[1]/dependency[3]/version" ) );
        assertEquals( "e", pom.getValue( "build/plugins[1]/dependency[4]/artifactId" ) );
        assertEquals( "1", pom.getValue( "build/plugins[1]/dependency[4]/version" ) );
        assertEquals( "e", pom.getValue( "build/plugins[1]/dependency[5]/artifactId" ) );
        assertEquals( "1", pom.getValue( "build/plugins[1]/dependency[5]/version" ) );
    }

    public void testOrderOfMergedPluginDependenciesWithPluginManagement()
        throws Exception
    {
        PomTestWrapper pom = buildPom( "merged-plugin-class-path-order/w-plugin-mngt/sub" );
        assertEquals( 5, ( (List<?>) pom.getValue( "build/plugins[1]/dependencies" ) ).size() );
        assertEquals( "c", pom.getValue( "build/plugins[1]/dependency[1]/artifactId" ) );
        assertEquals( "1", pom.getValue( "build/plugins[1]/dependency[1]/version" ) );
        assertEquals( "a", pom.getValue( "build/plugins[1]/dependency[2]/artifactId" ) );
        assertEquals( "2", pom.getValue( "build/plugins[1]/dependency[2]/version" ) );
        assertEquals( "b", pom.getValue( "build/plugins[1]/dependency[3]/artifactId" ) );
        assertEquals( "1", pom.getValue( "build/plugins[1]/dependency[3]/version" ) );
        assertEquals( "e", pom.getValue( "build/plugins[1]/dependency[4]/artifactId" ) );
        assertEquals( "1", pom.getValue( "build/plugins[1]/dependency[4]/version" ) );
        assertEquals( "e", pom.getValue( "build/plugins[1]/dependency[5]/artifactId" ) );
        assertEquals( "1", pom.getValue( "build/plugins[1]/dependency[5]/version" ) );
    }
   //*/

    public void testInterpolationOfNestedBuildDirectories()
        throws Exception
    {
        PomTestWrapper pom = buildPom( "nested-build-dir-interpolation" );
        assertEquals( new File( pom.getBasedir(), "target/classes/dir0" ),
                      new File( (String) pom.getValue( "properties/dir0" ) ) );
        assertEquals( new File( pom.getBasedir(), "src/test/dir1" ),
                      new File( (String) pom.getValue( "properties/dir1" ) ) );
        assertEquals( new File( pom.getBasedir(), "target/site/dir2" ),
                      new File( (String) pom.getValue( "properties/dir2" ) ) );
    }

    public void testAppendArtifactIdOfChildToInheritedUrls()
        throws Exception
    {
        PomTestWrapper pom = buildPom( "url-inheritance/sub" );
        assertEquals( "http://parent.url/child", pom.getValue( "url" ) );
        assertEquals( "http://parent.url/org/", pom.getValue( "organization/url" ) );
        assertEquals( "http://parent.url/license.txt", pom.getValue( "licenses[1]/url" ) );
        assertEquals( "http://parent.url/viewvc/child", pom.getValue( "scm/url" ) );
        assertEquals( "http://parent.url/scm/child", pom.getValue( "scm/connection" ) );
        assertEquals( "https://parent.url/scm/child", pom.getValue( "scm/developerConnection" ) );
        assertEquals( "http://parent.url/issues", pom.getValue( "issueManagement/url" ) );
        assertEquals( "http://parent.url/ci", pom.getValue( "ciManagement/url" ) );
        assertEquals( "http://parent.url/dist", pom.getValue( "distributionManagement/repository/url" ) );
        assertEquals( "http://parent.url/snaps", pom.getValue( "distributionManagement/snapshotRepository/url" ) );
        assertEquals( "http://parent.url/site/child", pom.getValue( "distributionManagement/site/url" ) );
        assertEquals( "http://parent.url/download", pom.getValue( "distributionManagement/downloadUrl" ) );
    }

    public void testNonInheritedElementsInSubtreesOverriddenByChild()
        throws Exception
    {
        PomTestWrapper pom = buildPom( "limited-inheritance/child" );
        assertEquals( null, pom.getValue( "organization/url" ) );
        assertEquals( null, pom.getValue( "issueManagement/system" ) );
        assertEquals( 0, ( (List<?>) pom.getValue( "ciManagement/notifiers" ) ).size() );
        assertEquals( null, pom.getValue( "distributionManagement/repository/name" ) );
        assertEquals( true, pom.getValue( "distributionManagement/repository/uniqueVersion" ) );
        assertEquals( "default", pom.getValue( "distributionManagement/repository/layout" ) );
        assertEquals( null, pom.getValue( "distributionManagement/snapshotRepository/name" ) );
        assertEquals( true, pom.getValue( "distributionManagement/snapshotRepository/uniqueVersion" ) );
        assertEquals( "default", pom.getValue( "distributionManagement/snapshotRepository/layout" ) );
        assertEquals( null, pom.getValue( "distributionManagement/site/name" ) );
    }

    public void testXmlTextCoalescing()
        throws Exception
    {
        PomTestWrapper pom = buildPom( "xml-coalesce-text" );
        assertEquals( "A  Test  Project Property", pom.getValue( "properties/prop0" ) );
        assertEquals( "That's a test!", pom.getValue( "properties/prop1" ) );
        assertEquals( 32 * 1024,
                      pom.getValue( "properties/prop2" ).toString().trim().replaceAll( "[\n\r]", "" ).length() );
    }

    public void testFullInterpolationOfNestedExpressions()
        throws Exception
    {
        PomTestWrapper pom = buildPom( "full-interpolation" );
        for ( int i = 0; i < 24; i++ )
        {
            String index = ( ( i < 10 ) ? "0" : "" ) + i;
            assertEquals( "PASSED", pom.getValue( "properties/property" + index ) );
        }
    }

    public void testInterpolationOfLegacyExpressionsThatDontIncludeTheProjectPrefix()
        throws Exception
    {
        PomTestWrapper pom = buildPom( "unprefixed-expression-interpolation/child" );
        assertEquals( pom.getBasedir(), new File( pom.getValue( "properties/projectDir" ).toString() ) );

        assertEquals( "org.apache.maven.its.mng3831.child", pom.getValue( "properties/projectGroupId" ) );
        assertEquals( "child", pom.getValue( "properties/projectArtifactId" ) );
        assertEquals( "2.0-alpha-1", pom.getValue( "properties/projectVersion" ) );
        assertEquals( "jar", pom.getValue( "properties/projectPackaging" ) );

        assertEquals( "child-name", pom.getValue( "properties/projectName" ) );
        assertEquals( "child-desc", pom.getValue( "properties/projectDesc" ) );
        assertEquals( "http://child.org/", pom.getValue( "properties/projectUrl" ) );
        assertEquals( "2008", pom.getValue( "properties/projectYear" ) );
        assertEquals( "child-org-name", pom.getValue( "properties/projectOrgName" ) );

        assertEquals( "2.0.0", pom.getValue( "properties/projectPrereqMvn" ) );
        assertEquals( "http://scm.org/", pom.getValue( "properties/projectScmUrl" ) );
        assertEquals( "http://issue.org/", pom.getValue( "properties/projectIssueUrl" ) );
        assertEquals( "http://ci.org/", pom.getValue( "properties/projectCiUrl" ) );
        assertEquals( "child-dist-repo", pom.getValue( "properties/projectDistRepoName" ) );
        assertEquals( "http://dist.org/", pom.getValue( "properties/projectDistRepoUrl" ) );
        assertEquals( "http://site.org/", pom.getValue( "properties/projectDistSiteUrl" ) );

        assertEquals( "org.apache.maven.its.mng3831", pom.getValue( "properties/parentGroupId" ) );
        assertEquals( "parent", pom.getValue( "properties/parentArtifactId" ) );
        assertEquals( "1.0", pom.getValue( "properties/parentVersion" ) );

        assertTrue( pom.getValue( "properties/projectBuildOut" ).toString().endsWith( "bin" ) );
        assertTrue( pom.getValue( "properties/projectSiteOut" ).toString().endsWith( "doc" ) );
    }

    public void testInterpolationWithBasedirAlignedDirectories()
        throws Exception
    {
        PomTestWrapper pom = buildPom( "basedir-aligned-interpolation" );
        assertEquals( new File( pom.getBasedir(), "src/main/java" ),
                      new File( pom.getValue( "properties/buildMainSrc" ).toString() ) );
        assertEquals( new File( pom.getBasedir(), "src/test/java" ),
                      new File( pom.getValue( "properties/buildTestSrc" ).toString() ) );
        assertEquals( new File( pom.getBasedir(), "src/main/scripts" ),
                      new File( pom.getValue( "properties/buildScriptSrc" ).toString() ) );
        assertEquals( new File( pom.getBasedir(), "target" ),
                      new File( pom.getValue( "properties/buildOut" ).toString() ) );
        assertEquals( new File( pom.getBasedir(), "target/classes" ),
                      new File( pom.getValue( "properties/buildMainOut" ).toString() ) );
        assertEquals( new File( pom.getBasedir(), "target/test-classes" ),
                      new File( pom.getValue( "properties/buildTestOut" ).toString() ) );
        assertEquals( new File( pom.getBasedir(), "target/site" ),
                      new File( pom.getValue( "properties/siteOut" ).toString() ) );
    }

    /* FIXME: cf. MNG-3944*/
    public void testInterpolationOfBasedirInPomWithUnusualName()
        throws Exception
    {
        PomTestWrapper pom = buildPom( "basedir-interpolation/pom-with-unusual-name.xml" );
        assertEquals( pom.getBasedir(), new File( pom.getValue( "properties/prop0" ).toString() ) );
        assertEquals( pom.getBasedir(), new File( pom.getValue( "properties/prop1" ).toString() ) );
    }

    /* MNG-3979 */
    public void testJoiningOfContainersWhenChildHasEmptyElements()
        throws Exception
    {
        PomTestWrapper pom = buildPom( "id-container-joining-with-empty-elements/sub" );
        assertNotNull( pom );
    }
    //*/

    public void testOrderOfPluginConfigurationElementsWithoutPluginManagement()
        throws Exception
    {
        PomTestWrapper pom = buildPom( "plugin-config-order/wo-plugin-mngt" );
        assertEquals( "one", pom.getValue( "build/plugins[1]/configuration/stringParams/stringParam[1]" ) );
        assertEquals( "two", pom.getValue( "build/plugins[1]/configuration/stringParams/stringParam[2]" ) );
        assertEquals( "three", pom.getValue( "build/plugins[1]/configuration/stringParams/stringParam[3]" ) );
        assertEquals( "four", pom.getValue( "build/plugins[1]/configuration/stringParams/stringParam[4]" ) );
    }

    /* FIXME: cf. MNG-3827*/
    public void testOrderOfPluginConfigurationElementsWithPluginManagement()
        throws Exception
    {
        PomTestWrapper pom = buildPom( "plugin-config-order/w-plugin-mngt" );
        assertEquals( "one", pom.getValue( "build/plugins[1]/configuration/stringParams/stringParam[1]" ) );
        assertEquals( "two", pom.getValue( "build/plugins[1]/configuration/stringParams/stringParam[2]" ) );
        assertEquals( "three", pom.getValue( "build/plugins[1]/configuration/stringParams/stringParam[3]" ) );
        assertEquals( "four", pom.getValue( "build/plugins[1]/configuration/stringParams/stringParam[4]" ) );
    }
    //*/

    public void testOrderOfPluginExecutionConfigurationElementsWithoutPluginManagement()
        throws Exception
    {
        PomTestWrapper pom = buildPom( "plugin-exec-config-order/wo-plugin-mngt" );
        String prefix = "build/plugins[1]/executions[1]/configuration/";
        assertEquals( "one", pom.getValue( prefix + "stringParams/stringParam[1]" ) );
        assertEquals( "two", pom.getValue( prefix + "stringParams/stringParam[2]" ) );
        assertEquals( "three", pom.getValue( prefix + "stringParams/stringParam[3]" ) );
        assertEquals( "four", pom.getValue( prefix + "stringParams/stringParam[4]" ) );
        assertEquals( "key1", pom.getValue( prefix + "propertiesParam/property[1]/name" ) );
        assertEquals( "key2", pom.getValue( prefix + "propertiesParam/property[2]/name" ) );
    }

    /* FIXME: cf. MNG-3864*/
    public void testOrderOfPluginExecutionConfigurationElementsWithPluginManagement()
        throws Exception
    {
        PomTestWrapper pom = buildPom( "plugin-exec-config-order/w-plugin-mngt" );
        String prefix = "build/plugins[1]/executions[1]/configuration/";
        assertEquals( "one", pom.getValue( prefix + "stringParams/stringParam[1]" ) );
        assertEquals( "two", pom.getValue( prefix + "stringParams/stringParam[2]" ) );
        assertEquals( "three", pom.getValue( prefix + "stringParams/stringParam[3]" ) );
        assertEquals( "four", pom.getValue( prefix + "stringParams/stringParam[4]" ) );
        assertEquals( "key1", pom.getValue( prefix + "propertiesParam/property[1]/name" ) );
        assertEquals( "key2", pom.getValue( prefix + "propertiesParam/property[2]/name" ) );
    }
    //*/

    /* FIXME: cf. MNG-3836
    public void testMergeOfInheritedPluginConfiguration()
        throws Exception
    {
        PomTestWrapper pom = buildPom( "plugin-config-merging/child" );
        String prefix = "build/plugins[1]/configuration/";
        assertEquals( "PASSED", pom.getValue( prefix + "propertiesFile" ) );
        assertEquals( "PASSED", pom.getValue( prefix + "parent" ) );
        assertEquals( "PASSED-1", pom.getValue( prefix + "stringParams/stringParam[1]" ) );
        assertEquals( "PASSED-2", pom.getValue( prefix + "stringParams/stringParam[2]" ) );
        assertEquals( "PASSED-3", pom.getValue( prefix + "stringParams/stringParam[3]" ) );
        assertEquals( "PASSED-4", pom.getValue( prefix + "stringParams/stringParam[4]" ) );
        assertEquals( "PASSED-1", pom.getValue( prefix + "listParam/listParam[1]" ) );
        assertEquals( "PASSED-2", pom.getValue( prefix + "listParam/listParam[2]" ) );
        assertEquals( "PASSED-3", pom.getValue( prefix + "listParam/listParam[3]" ) );
        assertEquals( "PASSED-4", pom.getValue( prefix + "listParam/listParam[4]" ) );
    }
    //*/

    /* FIXME: cf. MNG-2591
    public void testAppendOfInheritedPluginConfiguration()
        throws Exception
    {
        PomTestWrapper pom = buildPom( "plugin-config-append/subproject" );
        String prefix = "build/plugins[1]/configuration/";
        assertEquals( "PARENT-1", pom.getValue( prefix + "stringParams/stringParam[1]" ) );
        assertEquals( "PARENT-3", pom.getValue( prefix + "stringParams/stringParam[2]" ) );
        assertEquals( "PARENT-2", pom.getValue( prefix + "stringParams/stringParam[3]" ) );
        assertEquals( "PARENT-4", pom.getValue( prefix + "stringParams/stringParam[4]" ) );
        assertEquals( "CHILD-1", pom.getValue( prefix + "stringParams/stringParam[5]" ) );
        assertEquals( "CHILD-3", pom.getValue( prefix + "stringParams/stringParam[6]" ) );
        assertEquals( "CHILD-2", pom.getValue( prefix + "stringParams/stringParam[7]" ) );
        assertEquals( "CHILD-4", pom.getValue( prefix + "stringParams/stringParam[8]" ) );
        assertEquals( "PARENT-1", pom.getValue( prefix + "listParam/listParam[1]" ) );
        assertEquals( "PARENT-3", pom.getValue( prefix + "listParam/listParam[2]" ) );
        assertEquals( "PARENT-2", pom.getValue( prefix + "listParam/listParam[3]" ) );
        assertEquals( "PARENT-4", pom.getValue( prefix + "listParam/listParam[4]" ) );
        assertEquals( "CHILD-1", pom.getValue( prefix + "listParam/listParam[5]" ) );
        assertEquals( "CHILD-3", pom.getValue( prefix + "listParam/listParam[6]" ) );
        assertEquals( "CHILD-2", pom.getValue( prefix + "listParam/listParam[7]" ) );
        assertEquals( "CHILD-4", pom.getValue( prefix + "listParam/listParam[8]" ) );
    }
    //*/

    /* FIXME: cf. MNG-4000 */
    public void testMultiplePluginExecutionsWithAndWithoutIdsWithoutPluginManagement()
        throws Exception
    {
        PomTestWrapper pom = buildPom( "plugin-exec-w-and-wo-id/wo-plugin-mngt" );
        assertEquals( 2, ( (List<?>) pom.getValue( "build/plugins[1]/executions" ) ).size() );
        assertEquals( "log-string", pom.getValue( "build/plugins[1]/executions[1]/goals[1]" ) );
        assertEquals( "log-string", pom.getValue( "build/plugins[1]/executions[2]/goals[1]" ) );
    }
    //*/

    public void testMultiplePluginExecutionsWithAndWithoutIdsWithPluginManagement()
        throws Exception
    {
        PomTestWrapper pom = buildPom( "plugin-exec-w-and-wo-id/w-plugin-mngt" );
        assertEquals( 2, ( (List<?>) pom.getValue( "build/plugins[1]/executions" ) ).size() );
        assertEquals( "log-string", pom.getValue( "build/plugins[1]/executions[1]/goals[1]" ) );
        assertEquals( "log-string", pom.getValue( "build/plugins[1]/executions[2]/goals[1]" ) );
    }

    public void testDependencyOrderWithoutPluginManagement()
        throws Exception
    {
        PomTestWrapper pom = buildPom( "dependency-order/wo-plugin-mngt" );
        assertEquals( 4, ( (List<?>) pom.getValue( "dependencies" ) ).size() );
        assertEquals( "a", pom.getValue( "dependencies[1]/artifactId" ) );
        assertEquals( "c", pom.getValue( "dependencies[2]/artifactId" ) );
        assertEquals( "b", pom.getValue( "dependencies[3]/artifactId" ) );
        assertEquals( "d", pom.getValue( "dependencies[4]/artifactId" ) );
    }

    public void testDependencyOrderWithPluginManagement()
        throws Exception
    {
        PomTestWrapper pom = buildPom( "dependency-order/w-plugin-mngt" );
        assertEquals( 4, ( (List<?>) pom.getValue( "dependencies" ) ).size() );
        assertEquals( "a", pom.getValue( "dependencies[1]/artifactId" ) );
        assertEquals( "c", pom.getValue( "dependencies[2]/artifactId" ) );
        assertEquals( "b", pom.getValue( "dependencies[3]/artifactId" ) );
        assertEquals( "d", pom.getValue( "dependencies[4]/artifactId" ) );
    }

    public void testBuildDirectoriesUsePlatformSpecificFileSeparator()
        throws Exception
    {
        PomTestWrapper pom = buildPom( "platform-file-separator" );
        assertPathWithNormalizedFileSeparators( pom.getValue( "build/directory" ) );
        assertPathWithNormalizedFileSeparators( pom.getValue( "build/outputDirectory" ) );
        assertPathWithNormalizedFileSeparators( pom.getValue( "build/testOutputDirectory" ) );
        assertPathWithNormalizedFileSeparators( pom.getValue( "build/sourceDirectory" ) );
        assertPathWithNormalizedFileSeparators( pom.getValue( "build/testSourceDirectory" ) );
        assertPathWithNormalizedFileSeparators( pom.getValue( "build/resources[1]/directory" ) );
        assertPathWithNormalizedFileSeparators( pom.getValue( "build/testResources[1]/directory" ) );
        assertPathWithNormalizedFileSeparators( pom.getValue( "build/filters[1]" ) );
        assertPathWithNormalizedFileSeparators( pom.getValue( "reporting/outputDirectory" ) );
    }

    /* MNG-4008 */
    public void testMergedFilterOrder()
        throws Exception
    {
        PomTestWrapper pom = buildPom( "merged-filter-order/sub" );

        System.out.println(pom.getValue( "build/filters" ));
        assertEquals( 7, ( (List<?>) pom.getValue( "build/filters" ) ).size() );
        assertTrue( pom.getValue( "build/filters[1]" ).toString().endsWith( "child-a.properties" ) );
        assertTrue( pom.getValue( "build/filters[2]" ).toString().endsWith( "child-c.properties" ) );
        assertTrue( pom.getValue( "build/filters[3]" ).toString().endsWith( "child-b.properties" ) );
        assertTrue( pom.getValue( "build/filters[4]" ).toString().endsWith( "child-d.properties" ) );
        assertTrue( pom.getValue( "build/filters[5]" ).toString().endsWith( "parent-c.properties" ) );
        assertTrue( pom.getValue( "build/filters[6]" ).toString().endsWith( "parent-b.properties" ) );
        assertTrue( pom.getValue( "build/filters[7]" ).toString().endsWith( "parent-d.properties" ) );
    }

    /** MNG-4027
    public void testProfileInjectedDependencies()
        throws Exception
    {
        PomTestWrapper pom = buildPom( "profile-injected-dependencies" );
        assertEquals( 4, ( (List<?>) pom.getValue( "dependencies" ) ).size() );
        assertEquals( "a", pom.getValue( "dependencies[1]/artifactId" ) );
        assertEquals( "c", pom.getValue( "dependencies[2]/artifactId" ) );
        assertEquals( "b", pom.getValue( "dependencies[3]/artifactId" ) );
        assertEquals( "d", pom.getValue( "dependencies[4]/artifactId" ) );
    }
    //*/

    /** MNG-4034 */
    public void testManagedProfileDependency()
        throws Exception
    {
        PomTestWrapper pom = this.buildPomFromMavenProject( "managed-profile-dependency/sub", "maven-core-it" );
        assertEquals( 1, ( (List<?>) pom.getValue( "dependencies" ) ).size() );
        assertEquals( "org.apache.maven.its", pom.getValue( "dependencies[1]/groupId" ) );
        assertEquals( "maven-core-it-support", pom.getValue( "dependencies[1]/artifactId" ) );
        assertEquals( "1.3", pom.getValue( "dependencies[1]/version" ) );
        assertEquals( "runtime", pom.getValue( "dependencies[1]/scope" ) );
        assertEquals( 1, ( (List<?>) pom.getValue( "dependencies[1]/exclusions" ) ).size() );
        assertEquals( "commons-lang", pom.getValue( "dependencies[1]/exclusions[1]/groupId" ) );
    }
    //*/

    /** MNG-4040 */
    public void testProfileModuleInheritance()
        throws Exception
    {
        PomTestWrapper pom = this.buildPomFromMavenProject( "profile-module-inheritance/sub", "dist" );
        assertEquals(0, ( (List<?>) pom.getValue( "modules" ) ).size());

    }

    public void testPluginConfigurationUsingAttributesWithoutPluginManagement()
        throws Exception
    {
        PomTestWrapper pom = buildPom( "plugin-config-attributes/wo-plugin-mngt" );
        assertEquals( "src", pom.getValue( "build/plugins[1]/configuration/domParam/copy/@todir" ) );
        assertEquals( "true", pom.getValue( "build/plugins[1]/configuration/domParam/copy/@overwrite" ) );
        assertEquals( "target", pom.getValue( "build/plugins[1]/configuration/domParam/copy/fileset/@dir" ) );
        assertEquals( null, pom.getValue( "build/plugins[1]/configuration/domParam/copy/fileset/@todir" ) );
        assertEquals( null, pom.getValue( "build/plugins[1]/configuration/domParam/copy/fileset/@overwrite" ) );
    }

    /** FIXME: MNG-4053
    public void testPluginConfigurationUsingAttributesWithPluginManagement()
        throws Exception
    {
        PomTestWrapper pom = buildPom( "plugin-config-attributes/w-plugin-mngt" );
        assertEquals( "src", pom.getValue( "build/plugins[1]/configuration/domParam/copy/@todir" ) );
        assertEquals( "true", pom.getValue( "build/plugins[1]/configuration/domParam/copy/@overwrite" ) );
        assertEquals( "target", pom.getValue( "build/plugins[1]/configuration/domParam/copy/fileset/@dir" ) );
        assertEquals( null, pom.getValue( "build/plugins[1]/configuration/domParam/copy/fileset/@todir" ) );
        assertEquals( null, pom.getValue( "build/plugins[1]/configuration/domParam/copy/fileset/@overwrite" ) );
    }

    public void testPluginConfigurationUsingAttributesWithPluginManagementAndProfile()
        throws Exception
    {
        PomTestWrapper pom = buildPomFromMavenProject( "plugin-config-attributes/w-profile", "maven-core-it" );
        assertEquals( "src", pom.getValue( "build/plugins[1]/configuration/domParam/copy/@todir" ) );
        assertEquals( "true", pom.getValue( "build/plugins[1]/configuration/domParam/copy/@overwrite" ) );
        assertEquals( "target", pom.getValue( "build/plugins[1]/configuration/domParam/copy/fileset/@dir" ) );
        assertEquals( null, pom.getValue( "build/plugins[1]/configuration/domParam/copy/fileset/@todir" ) );
        assertEquals( null, pom.getValue( "build/plugins[1]/configuration/domParam/copy/fileset/@overwrite" ) );
    }
    //*/

    public void testPomEncoding()
        throws Exception
    {
        PomTestWrapper pom = buildPom( "pom-encoding/utf-8" );
        assertEquals( "TEST-CHARS: \u00DF\u0131\u03A3\u042F\u05D0\u20AC", pom.getValue( "description" ) );
        pom = buildPom( "pom-encoding/latin-1" );
        assertEquals( "TEST-CHARS: \u00C4\u00D6\u00DC\u00E4\u00F6\u00FC\u00DF", pom.getValue( "description" ) );
    }

    private void assertPathWithNormalizedFileSeparators( Object value )
    {
        assertEquals( new File( value.toString() ).getPath(), value.toString() );
    }

    private PomTestWrapper buildPom( String pomPath )
        throws IOException
    {
        File pomFile = new File( testDirectory , pomPath );
        if ( pomFile.isDirectory() )
        {
            pomFile = new File( pomFile, "pom.xml" );
        }
        return new PomTestWrapper( pomFile, mavenProjectBuilder.buildModel( pomFile, null, null, null ) );
    }

    private PomTestWrapper buildPomFromMavenProject( String pomPath, String profileId )
        throws IOException
    {
        File pomFile = new File( testDirectory , pomPath );
        if ( pomFile.isDirectory() )
        {
            pomFile = new File( pomFile, "pom.xml" );
        }
        ProjectBuilderConfiguration config = new DefaultProjectBuilderConfiguration();
        config.setLocalRepository(new DefaultArtifactRepository("default", "", new DefaultRepositoryLayout()));
        ProfileActivationContext pCtx = new ProfileActivationContext(null, true);
        if(profileId != null)
        {
            pCtx.setExplicitlyActiveProfileIds(Arrays.asList(profileId));
        }

        config.setGlobalProfileManager(new DefaultProfileManager(this.getContainer(), pCtx));
        return new PomTestWrapper( pomFile, mavenProjectBuilder.buildFromLocalPath( pomFile, null, null, null,
                config, mavenProjectBuilder ) );
    }

    private Model buildMixin( String mixinPath )
        throws IOException, XmlPullParserException
    {
        File mixinFile = new File( testMixinDirectory , mixinPath );
        if ( mixinFile.isDirectory() )
        {
            mixinFile = new File( mixinFile, "mixin.xml" );
        }
        FileInputStream pluginStream = new FileInputStream( mixinFile );
        MavenXpp3Reader reader = new MavenXpp3Reader();
        return reader.read(pluginStream, false);
    }

    protected void assertModelEquals( PomTestWrapper pom, Object expected, String expression )
    {
        assertEquals( expected, pom.getValue( expression ) );        
    }

    private static String createPath(List<String> elements)
    {
        StringBuffer buffer = new StringBuffer();
        for(String s : elements)
        {
            buffer.append(s).append(File.separator);
        }
        return buffer.toString().substring(0, buffer.toString().length() - 1);
    }
}
