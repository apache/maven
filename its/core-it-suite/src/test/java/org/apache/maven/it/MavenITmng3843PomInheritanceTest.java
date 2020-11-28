package org.apache.maven.it;

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

import org.apache.maven.it.util.ResourceExtractor;

import java.io.File;
import java.util.Collection;
import java.util.Properties;
import java.util.TreeSet;

/**
 * This is a test set for <a href="https://issues.apache.org/jira/browse/MNG-3843">MNG-3843</a>.
 *
 * @author Benjamin Bentmann
 *
 */
public class MavenITmng3843PomInheritanceTest
    extends AbstractMavenIntegrationTestCase
{

    public MavenITmng3843PomInheritanceTest()
    {
        super( ALL_MAVEN_VERSIONS );
    }

    /**
     * Test various inheritance scenarios.
     */
    public void testitMNG3843()
        throws Exception
    {
        File testDir = ResourceExtractor.simpleExtractResources( getClass(), "/mng-3843" );

        testDir = testDir.getCanonicalFile();

        Verifier verifier = newVerifier( testDir.getAbsolutePath() );
        verifier.setAutoclean( false );
        verifier.deleteDirectory( "test-1/target" );
        verifier.deleteDirectory( "test-2/target" );
        verifier.deleteDirectory( "test-2/child-1/target" );
        verifier.deleteDirectory( "test-2/child-2/target" );
        verifier.deleteDirectory( "test-3/sub-parent/child-a/target" );
        verifier.addCliOption( "--settings" );
        verifier.addCliOption( "settings.xml" );
        verifier.executeGoal( "validate" );
        verifier.verifyErrorFreeLog();
        verifier.resetStreams();

        Properties props;
        File basedir;

        basedir = new File( verifier.getBasedir(), "test-1" );
        props = verifier.loadProperties( "test-1/target/pom.properties" );
        assertEquals( "org.apache.maven.its.mng3843", props.getProperty( "project.groupId" ) );
        assertEquals( "test-1", props.getProperty( "project.artifactId" ) );
        assertEquals( "0.1", props.getProperty( "project.version" ) );
        assertEquals( "jar", props.getProperty( "project.packaging" ) );
        assertEquals( "test", props.getProperty( "project.name", "" ) );
        assertEquals( "", props.getProperty( "project.description", "" ) );
        assertEquals( "", props.getProperty( "project.url", "" ) );
        assertEquals( "", props.getProperty( "project.inceptionYear", "" ) );
        assertEquals( "", props.getProperty( "project.build.defaultGoal", "" ) );
        assertMissing( props, "project.properties." );
        assertMissing( props, "project.prerequisites." );
        assertMissing( props, "project.modules." );
        assertMissing( props, "project.licenses." );
        assertMissing( props, "project.developers." );
        assertMissing( props, "project.contributors." );
        assertMissing( props, "project.mailingLists." );
        assertMissing( props, "project.organization." );
        assertMissing( props, "project.scm." );
        assertMissing( props, "project.ciManagement." );
        assertMissing( props, "project.issueManagement." );
        assertMissing( props, "project.distributionManagement." );
        assertMissing( props, "project.profiles." );
        assertEquals( "test-1-0.1", props.getProperty( "project.build.finalName" ) );
        assertPathEquals( basedir, "src/main/java", props.getProperty( "project.build.sourceDirectory" ) );
        assertPathEquals( basedir, "src/test/java", props.getProperty( "project.build.testSourceDirectory" ) );
        assertPathEquals( basedir, "src/main/scripts", props.getProperty( "project.build.scriptSourceDirectory" ) );
        if ( matchesVersionRange( "[4.0.0-alpha-1,)" ) )
        {
            assertEquals( "2", props.getProperty( "project.build.resources" ) );
            assertEquals( "2", props.getProperty( "project.build.testResources" ) );
        }
        else
        {
            assertEquals( "1", props.getProperty( "project.build.resources" ) );
            assertEquals( "1", props.getProperty( "project.build.testResources" ) );
        }
        assertPathEquals( basedir, "src/main/resources", props.getProperty( "project.build.resources.0.directory" ) );
        assertPathEquals( basedir, "src/test/resources",
                          props.getProperty( "project.build.testResources.0.directory" ) );
        if ( matchesVersionRange( "[4.0.0-alpha-1,)" ) )
        {
            assertPathEquals( basedir, "src/main/resources-filtered", props.getProperty( "project.build.resources.1.directory" ) );
            assertPathEquals( basedir, "src/test/resources-filtered",
                              props.getProperty( "project.build.testResources.1.directory" ) );
        }
        assertPathEquals( basedir, "target", props.getProperty( "project.build.directory" ) );
        assertPathEquals( basedir, "target/classes", props.getProperty( "project.build.outputDirectory" ) );
        assertPathEquals( basedir, "target/test-classes", props.getProperty( "project.build.testOutputDirectory" ) );
        assertPathEquals( basedir, "target/site", props.getProperty( "project.reporting.outputDirectory" ) );
        assertEquals( "false", props.getProperty( "project.reporting.excludeDefaults" ) );
        assertTrue( Integer.parseInt( props.getProperty( "project.repositories" ) ) > 0 );
        if ( matchesVersionRange( "(,3.0-alpha-3)" ) )
        {
            // 3.x will provide the lifecycle bindings in the effective model, don't count these
            assertEquals( "1", props.getProperty( "project.build.plugins" ) );
        }
        assertMissing( props, "project.dependencies." );
        assertMissing( props, "project.dependencyManagement." );

        basedir = new File( verifier.getBasedir(), "test-2" );
        props = verifier.loadProperties( "test-2/target/pom.properties" );

        basedir = new File( verifier.getBasedir(), "test-2/child-1" );
        props = verifier.loadProperties( "test-2/child-1/target/pom.properties" );
        assertEquals( "org.apache.maven.its.mng3843", props.getProperty( "project.groupId" ) );
        assertEquals( "child-1", props.getProperty( "project.artifactId" ) );
        assertEquals( "0.1", props.getProperty( "project.version" ) );
        assertEquals( "jar", props.getProperty( "project.packaging" ) );
        assertFalse( "parent-name".equals( props.getProperty( "project.name" ) ) );
        assertEquals( "parent-description", props.getProperty( "project.description", "" ) );
        assertUrlCommon( "http://parent.url", props.getProperty( "project.url", "" ) );
        assertEquals( "2008", props.getProperty( "project.inceptionYear", "" ) );
        assertEquals( "initialize", props.getProperty( "project.build.defaultGoal" ) );
        assertEquals( "parent-property", props.getProperty( "project.properties.parentProperty" ) );
        assertMissing( props, "project.prerequisites." );
        assertMissing( props, "project.modules." );
        assertEquals( "1", props.getProperty( "project.licenses" ) );
        assertEquals( "http://parent.url/license", props.getProperty( "project.licenses.0.url" ) );
        assertEquals( "1", props.getProperty( "project.developers" ) );
        assertEquals( "parent-developer", props.getProperty( "project.developers.0.name" ) );
        assertEquals( "1", props.getProperty( "project.contributors" ) );
        assertEquals( "parent-contributor", props.getProperty( "project.contributors.0.name" ) );
        assertEquals( "1", props.getProperty( "project.mailingLists" ) );
        assertEquals( "parent-mailing-list", props.getProperty( "project.mailingLists.0.name" ) );
        assertEquals( "http://parent-org.url/", props.getProperty( "project.organization.url" ) );
        assertUrlCommon( "http://parent.url/trunk", props.getProperty( "project.scm.url" ) );
        assertUrlCommon( "http://parent.url/scm", props.getProperty( "project.scm.connection" ) );
        assertUrlCommon( "https://parent.url/scm", props.getProperty( "project.scm.developerConnection" ) );
        assertEquals( "http://parent.url/ci", props.getProperty( "project.ciManagement.url" ) );
        assertEquals( "http://parent.url/issues", props.getProperty( "project.issueManagement.url" ) );
        assertEquals( "http://parent.url/dist", props.getProperty( "project.distributionManagement.repository.url" ) );
        assertEquals( "http://parent.url/snaps",
                      props.getProperty( "project.distributionManagement.snapshotRepository.url" ) );
        assertUrlCommon( "http://parent.url/site", props.getProperty( "project.distributionManagement.site.url" ) );
        assertUrlCommon( "http://parent.url/download",
                         props.getProperty( "project.distributionManagement.downloadUrl" ) );
        if ( matchesVersionRange( "(2.0.2,)" ) )
        {
            assertMissing( props, "project.distributionManagement.relocation." );
        }
        assertMissing( props, "project.profiles." );
        assertEquals( "child-1-0.1", props.getProperty( "project.build.finalName" ) );
        assertPathEquals( basedir, "src/main", props.getProperty( "project.build.sourceDirectory" ) );
        assertPathEquals( basedir, "src/test", props.getProperty( "project.build.testSourceDirectory" ) );
        assertPathEquals( basedir, "src/scripts", props.getProperty( "project.build.scriptSourceDirectory" ) );
        assertEquals( "1", props.getProperty( "project.build.resources" ) );
        assertPathEquals( basedir, "res/main", props.getProperty( "project.build.resources.0.directory" ) );
        assertEquals( "1", props.getProperty( "project.build.testResources" ) );
        assertPathEquals( basedir, "res/test", props.getProperty( "project.build.testResources.0.directory" ) );
        assertPathEquals( basedir, "out", props.getProperty( "project.build.directory" ) );
        assertPathEquals( basedir, "out/main", props.getProperty( "project.build.outputDirectory" ) );
        assertPathEquals( basedir, "out/test", props.getProperty( "project.build.testOutputDirectory" ) );
        assertPathEquals( basedir, "site", props.getProperty( "project.reporting.outputDirectory" ) );
        if ( matchesVersionRange( "(2.0.9,2.1.0-M1),(2.1.0-M1,)" ) )
        {
            // MNG-1999
            assertEquals( "true", props.getProperty( "project.reporting.excludeDefaults" ) );
        }
        assertTrue( Integer.parseInt( props.getProperty( "project.repositories" ) ) > 1 );
        if ( matchesVersionRange( "(,3.0-alpha-3)" ) )
        {
            // 3.x will provide the lifecycle bindings in the effective model, don't count these
            assertEquals( "1", props.getProperty( "project.build.plugins" ) );
        }
        assertEquals( "1", props.getProperty( "project.dependencies" ) );
        assertEquals( "parent-dep-b", props.getProperty( "project.dependencies.0.artifactId" ) );
        assertEquals( "1", props.getProperty( "project.dependencyManagement.dependencies" ) );
        assertEquals( "parent-dep-a", props.getProperty( "project.dependencyManagement.dependencies.0.artifactId" ) );

        basedir = new File( verifier.getBasedir(), "test-2/child-2" );
        props = verifier.loadProperties( "test-2/child-2/target/pom.properties" );
        assertEquals( "org.apache.maven.its.mng3843.child", props.getProperty( "project.groupId" ) );
        assertEquals( "child-2", props.getProperty( "project.artifactId" ) );
        assertEquals( "0.2", props.getProperty( "project.version" ) );
        assertEquals( "jar", props.getProperty( "project.packaging" ) );
        assertEquals( "child-name", props.getProperty( "project.name" ) );
        assertEquals( "child-description", props.getProperty( "project.description", "" ) );
        assertUrlCommon( "http://child.url", props.getProperty( "project.url", "" ) );
        assertEquals( "2009", props.getProperty( "project.inceptionYear", "" ) );
        assertEquals( "validate", props.getProperty( "project.build.defaultGoal" ) );
        assertEquals( "parent-property", props.getProperty( "project.properties.parentProperty" ) );
        assertEquals( "child-property", props.getProperty( "project.properties.childProperty" ) );
        assertEquals( "child-override", props.getProperty( "project.properties.overriddenProperty" ) );
        assertEquals( "2.0.1", props.getProperty( "project.prerequisites.maven" ) );
        assertMissing( props, "project.modules." );
        assertEquals( "1", props.getProperty( "project.licenses" ) );
        assertEquals( "http://child.url/license", props.getProperty( "project.licenses.0.url" ) );
        assertEquals( "1", props.getProperty( "project.developers" ) );
        assertEquals( "child-developer", props.getProperty( "project.developers.0.name" ) );
        assertEquals( "1", props.getProperty( "project.contributors" ) );
        assertEquals( "child-contributor", props.getProperty( "project.contributors.0.name" ) );
        assertEquals( "1", props.getProperty( "project.mailingLists" ) );
        assertEquals( "child-mailing-list", props.getProperty( "project.mailingLists.0.name" ) );
        assertEquals( "http://child-org.url/", props.getProperty( "project.organization.url" ) );
        assertUrlCommon( "http://child.url/trunk", props.getProperty( "project.scm.url" ) );
        assertUrlCommon( "http://child.url/scm", props.getProperty( "project.scm.connection" ) );
        assertUrlCommon( "https://child.url/scm", props.getProperty( "project.scm.developerConnection" ) );
        assertEquals( "http://child.url/ci", props.getProperty( "project.ciManagement.url" ) );
        assertEquals( "http://child.url/issues", props.getProperty( "project.issueManagement.url" ) );
        assertEquals( "http://child.url/dist", props.getProperty( "project.distributionManagement.repository.url" ) );
        assertEquals( "http://child.url/snaps",
                      props.getProperty( "project.distributionManagement.snapshotRepository.url" ) );
        assertUrlCommon( "http://child.url/site", props.getProperty( "project.distributionManagement.site.url" ) );
        assertUrlCommon( "http://child.url/download",
                         props.getProperty( "project.distributionManagement.downloadUrl" ) );
        assertEquals( "child-reloc-msg", props.getProperty( "project.distributionManagement.relocation.message" ) );
        assertMissing( props, "project.profiles." );
        assertEquals( "coreit", props.getProperty( "project.build.finalName" ) );
        assertPathEquals( basedir, "sources/main", props.getProperty( "project.build.sourceDirectory" ) );
        assertPathEquals( basedir, "sources/test", props.getProperty( "project.build.testSourceDirectory" ) );
        assertPathEquals( basedir, "sources/scripts", props.getProperty( "project.build.scriptSourceDirectory" ) );
        assertEquals( "1", props.getProperty( "project.build.resources" ) );
        assertPathEquals( basedir, "resources/main", props.getProperty( "project.build.resources.0.directory" ) );
        assertEquals( "1", props.getProperty( "project.build.testResources" ) );
        assertPathEquals( basedir, "resources/test", props.getProperty( "project.build.testResources.0.directory" ) );
        assertPathEquals( basedir, "build", props.getProperty( "project.build.directory" ) );
        assertPathEquals( basedir, "build/main", props.getProperty( "project.build.outputDirectory" ) );
        assertPathEquals( basedir, "build/test", props.getProperty( "project.build.testOutputDirectory" ) );
        assertPathEquals( basedir, "docs", props.getProperty( "project.reporting.outputDirectory" ) );
        assertEquals( "false", props.getProperty( "project.reporting.excludeDefaults" ) );
        assertTrue( Integer.parseInt( props.getProperty( "project.repositories" ) ) > 1 );
        if ( matchesVersionRange( "(2.0.4,3.0-alpha-3)" ) )
        {
            // 3.x will provide the lifecycle bindings in the effective model, don't count these
            assertEquals( "1", props.getProperty( "project.build.plugins" ) );
        }
        assertEquals( "4", props.getProperty( "project.dependencies" ) );
        Collection<String> actualDeps = new TreeSet<>();
        actualDeps.add( props.getProperty( "project.dependencies.0.artifactId" ) );
        actualDeps.add( props.getProperty( "project.dependencies.1.artifactId" ) );
        actualDeps.add( props.getProperty( "project.dependencies.2.artifactId" ) );
        actualDeps.add( props.getProperty( "project.dependencies.3.artifactId" ) );
        Collection<String> expectedDeps = new TreeSet<>();
        expectedDeps.add( "parent-dep-b" );
        expectedDeps.add( "child-dep-b" );
        expectedDeps.add( "child-dep-c" );
        expectedDeps.add( "child-dep-d" );
        assertEquals( expectedDeps, actualDeps );
        assertEquals( "2", props.getProperty( "project.dependencyManagement.dependencies" ) );
        Collection<String> actualMngtDeps = new TreeSet<>();
        actualMngtDeps.add( props.getProperty( "project.dependencyManagement.dependencies.0.artifactId" ) );
        actualMngtDeps.add( props.getProperty( "project.dependencyManagement.dependencies.1.artifactId" ) );
        Collection<String> expectedMngtDeps = new TreeSet<>();
        expectedMngtDeps.add( "parent-dep-a" );
        expectedMngtDeps.add( "child-dep-a" );
        assertEquals( expectedMngtDeps, actualMngtDeps );

        basedir = new File( verifier.getBasedir(), "test-3/sub-parent/child-a" );
        props = verifier.loadProperties( "test-3/sub-parent/child-a/target/pom.properties" );
        assertEquals( "../pom.xml", props.getProperty( "project.originalModel.parent.relativePath" ) );
    }

    private void assertPathEquals( File basedir, String expected, String actual )
    {
        // NOTE: Basedir alignment is another issue, so don't test this here
        File actualFile = new File( actual );
        if ( actualFile.isAbsolute() )
        {
            assertEquals( new File( basedir, expected ), actualFile );
        }
        else
        {
            assertEquals( new File( expected ), actualFile );
        }
    }

    private void assertUrlCommon( String expected, String actual )
    {
        // NOTE: URL adjustment is a slightly different issue, so don't test here and merely check for common prefix
        assertTrue( "expected " + expected + " but was " + actual, actual.startsWith( expected ) );
    }

    private void assertMissing( Properties props, String prefix )
    {
        for ( Object o : props.keySet() )
        {
            String key = o.toString();
            assertFalse( "Found unexpected key: " + key, key.startsWith( prefix ) );
        }
    }

}
