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

import org.apache.maven.it.Verifier;
import org.apache.maven.it.util.ResourceExtractor;

import java.io.File;
import java.util.Iterator;
import java.util.Properties;

/**
 * This is a test set for <a href="http://jira.codehaus.org/browse/MNG-3843">MNG-3843</a>.
 * 
 * @author Benjamin Bentmann
 * @version $Id$
 */
public class MavenITmng3843PomInheritanceTest
    extends AbstractMavenIntegrationTestCase
{

    public MavenITmng3843PomInheritanceTest()
    {
    }

    /**
     * Test various inheritance scenarios.
     */
    public void testitMNG3843()
        throws Exception
    {
        File testDir = ResourceExtractor.simpleExtractResources( getClass(), "/mng-3843" );

        Verifier verifier = new Verifier( testDir.getAbsolutePath() );
        verifier.setAutoclean( false );
        verifier.deleteDirectory( "test-1/target" );
        verifier.deleteDirectory( "test-2/target" );
        verifier.deleteDirectory( "test-2/child-1/target" );
        verifier.deleteDirectory( "test-3/sub-parent/child-a/target" );
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
        assertEquals( "1", props.getProperty( "project.build.resources" ) );
        assertPathEquals( basedir, "src/main/resources", props.getProperty( "project.build.resources.0.directory" ) );
        assertEquals( "1", props.getProperty( "project.build.testResources" ) );
        assertPathEquals( basedir, "src/test/resources", props.getProperty( "project.build.testResources.0.directory" ) );
        assertPathEquals( basedir, "target", props.getProperty( "project.build.directory" ) );
        assertPathEquals( basedir, "target/classes", props.getProperty( "project.build.outputDirectory" ) );
        assertPathEquals( basedir, "target/test-classes", props.getProperty( "project.build.testOutputDirectory" ) );
        assertPathEquals( basedir, "target/site", props.getProperty( "project.reporting.outputDirectory" ) );
        assertEquals( "false", props.getProperty( "project.reporting.excludeDefaults" ) );
        assertTrue( Integer.parseInt( props.getProperty( "project.repositories" ) ) > 0 );
        assertEquals( "1", props.getProperty( "project.build.plugins" ) );
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
        assertEquals( "http://parent.url/snaps", props.getProperty( "project.distributionManagement.snapshotRepository.url" ) );
        assertUrlCommon( "http://parent.url/site", props.getProperty( "project.distributionManagement.site.url" ) );
        assertUrlCommon( "http://parent.url/download", props.getProperty( "project.distributionManagement.downloadUrl" ) );
        assertMissing( props, "project.distributionManagement.relocation." );
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
        assertEquals( "1", props.getProperty( "project.build.plugins" ) );
        assertEquals( "1", props.getProperty( "project.dependencies" ) );
        assertEquals( "parent-dep-b", props.getProperty( "project.dependencies.0.artifactId" ) );
        assertEquals( "1", props.getProperty( "project.dependencyManagement.dependencies" ) );
        assertEquals( "parent-dep-a", props.getProperty( "project.dependencyManagement.dependencies.0.artifactId" ) );

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
        for ( Iterator it = props.keySet().iterator(); it.hasNext(); )
        {
            String key = it.next().toString();
            assertFalse( "Found unexpected key: " + key, key.startsWith( prefix ) );
        }
    }

}
