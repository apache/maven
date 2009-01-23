package org.apache.maven.project.builder;

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
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.maven.MavenTools;
import org.apache.maven.artifact.Artifact;
import org.apache.maven.project.harness.PomTestWrapper;
import org.codehaus.plexus.PlexusTestCase;

public class PomConstructionTest
    extends PlexusTestCase
{

    private static String BASE_POM_DIR = "src/test/resources-project-builder";

    private ProjectBuilder projectBuilder;

    private MavenTools mavenTools;

    private PomArtifactResolver pomArtifactResolver;

    private File testDirectory;

    protected void setUp()
        throws Exception
    {
        testDirectory = new File( getBasedir(), BASE_POM_DIR );
        projectBuilder = lookup( ProjectBuilder.class );
        mavenTools = lookup( MavenTools.class );
        pomArtifactResolver = new PomArtifactResolver()
        {

            public void resolve( Artifact artifact )
                throws IOException
            {
                throw new IllegalStateException( "Parent POM should be locally reachable " + artifact );
            }

        };
    }

    // Some better conventions for the test poms needs to be created and each of these tests
    // that represent a verification of a specification item needs to be a couple lines at most.
    // The expressions help a lot, but we need a clean to pick up a directory of POMs, automatically load
    // them into a resolver, create the expression to extract the data to validate the Model, and the URI
    // to validate the properties. We also need a way to navigate from the Tex specification documents to
    // the test in question and vice versa. A little Eclipse plugin would do the trick.
    /*
    public void testThatExecutionsWithoutIdsAreMergedAndTheChildWins()
        throws Exception
    {
        File pom = new File( testDirectory, "micromailer/micromailer-1.0.3.pom" );
        PomArtifactResolver resolver = artifactResolver( "micromailer" );
        PomClassicDomainModel model = projectBuilder.buildModel( pom, null, resolver );
        // This should be 2
        //assertEquals( 2, model.getLineageCount() );
        PomTestWrapper tester = new PomTestWrapper( model );
        assertModelEquals( tester, "child-descriptor", "build/plugins[1]/executions[1]/goals[1]" );
    }
      */
    public void testErroneousJoiningOfDifferentPluginsWithEqualDependencies()
        throws Exception
    {
        PomTestWrapper pom = buildPom( "equal-plugin-deps" );
        assertEquals( "maven-it-plugin-a", pom.getValue( "build/plugins[1]/artifactId" ) );
        assertEquals( 1, ( (List<?>) pom.getValue( "build/plugins[1]/dependencies" ) ).size() );
        assertEquals( "maven-it-plugin-b", pom.getValue( "build/plugins[2]/artifactId" ) );
        assertEquals( 1, ( (List<?>) pom.getValue( "build/plugins[1]/dependencies" ) ).size() );
    }

    /** MNG-3821 -FIX---
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
    */
     /** MNG-3998 */
    public void testExecutionConfiguration()
        throws Exception
    {
        PomTestWrapper pom = buildPom( "execution-configuration" );
    }

    public void testSingleConfigurationInheritance()
        throws Exception
    {
        PomTestWrapper pom = buildPom( "single-configuration-inheritance" );
    }

    public void testConfigWithPluginManagement()
        throws Exception
    {
        PomTestWrapper pom = buildPom( "config-with-plugin-mng" );
    }

    /** MNG-3965 */
    public void testExecutionConfigurationSubcollections()
        throws Exception
    {
        PomTestWrapper pom = buildPom( "execution-configuration-subcollections" );
    }

    /** MNG- */
    public void testFoo()
        throws Exception
    {
        PomTestWrapper pom = buildPom( "foo/sub" );
    }

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

    /* FIXME: cf. MNG-3887
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
        assertEquals( "child-default", pom.getValue( "build/plugins[1]/executions[@id='default']/phase" ) );
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
   */

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

    /* FIXME: cf. MNG-3827
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

    /* FIXME: cf. MNG-3864
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

    private PomArtifactResolver artifactResolver( String basedir )
    {
        return new FileBasedPomArtifactResolver( new File( BASE_POM_DIR, basedir ) );
    }

    private PomTestWrapper buildPom( String pomPath )
        throws IOException
    {
        File pomFile = new File( testDirectory , pomPath );
        if ( pomFile.isDirectory() )
        {
            pomFile = new File( pomFile, "pom.xml" );
        }
        return new PomTestWrapper( pomFile, projectBuilder.buildModel( pomFile, null, pomArtifactResolver ) );
    }

    protected void assertModelEquals( PomTestWrapper pom, Object expected, String expression )
    {
        assertEquals( expected, pom.getValue( expression ) );        
    }
    
    // Need to get this to walk around a directory and automatically build up the artifact set. If we
    // follow some standard conventions this can be simple.
    class FileBasedPomArtifactResolver
        implements PomArtifactResolver
    {
        private Map<String,File> artifacts = new HashMap<String,File>();
                
        private File basedir;
                
        public FileBasedPomArtifactResolver( File basedir )
        {
            this.basedir = basedir;
                        
            for ( File file : basedir.listFiles() )
            {
                String fileName = file.getName();                
                if ( file.getName().endsWith( ".pom" ) )
                {
                    int i = fileName.indexOf( ".pom" );                    
                    String id = fileName.substring( 0, i );
                    artifacts.put( id, file );
                }
            }
        }

        public FileBasedPomArtifactResolver( Map<String, File> artifacts )
        {
            this.artifacts = artifacts;
        }

        public void resolve( Artifact artifact )
            throws IOException
        {
            String id = artifact.getArtifactId() + "-" + artifact.getVersion();
            artifact.setFile( artifacts.get( id  ) );
        }
    }
}
