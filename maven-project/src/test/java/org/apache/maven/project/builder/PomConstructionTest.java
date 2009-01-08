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

    public void testErroneousJoiningOfDifferentPluginsWithEqualDependencies()
        throws Exception
    {
        PomTestWrapper pom = buildPom( "equal-plugin-deps" );
        assertEquals( "maven-it-plugin-a", pom.getValue( "build/plugins[1]/artifactId" ) );
        assertEquals( 1, ( (List<?>) pom.getValue( "build/plugins[1]/dependencies" ) ).size() );
        assertEquals( "maven-it-plugin-b", pom.getValue( "build/plugins[2]/artifactId" ) );
        assertEquals( 1, ( (List<?>) pom.getValue( "build/plugins[1]/dependencies" ) ).size() );
    }

    /* FIXME: cf. MNG-3821
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

    /* FIXME: cf. MNG-3886
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

    /* FIXME: cf. MNG-3943
    public void testMergeOfPluginExecutionsWhenChildAndParentUseDifferentPluginVersions()
        throws Exception
    {
        PomTestWrapper pom = buildPom( "plugin-exec-merging-version-insensitive/sub" );
        assertEquals( 4, ( (List<?>) pom.getValue( "build/plugins[1]/executions" ) ).size() );
    }
    //*/

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

    /* FIXME: cf. MNG-3937
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

    /* FIXME: cf. MNG-3938 */
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
   

    private PomArtifactResolver artifactResolver( String basedir )
    {
        return new FileBasedPomArtifactResolver( new File( BASE_POM_DIR, basedir ) );
    }

    private PomTestWrapper buildPom( String pomPath )
        throws IOException
    {
        File pomFile = new File( testDirectory, pomPath );
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
