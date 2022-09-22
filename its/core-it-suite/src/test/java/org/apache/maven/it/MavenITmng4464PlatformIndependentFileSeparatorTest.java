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

import org.apache.maven.shared.verifier.util.ResourceExtractor;
import org.apache.maven.shared.verifier.Verifier;

import java.io.File;
import java.util.Properties;

/**
 * This is a test set for <a href="https://issues.apache.org/jira/browse/MNG-4464">MNG-4464</a>.
 *
 * @author Benjamin Bentmann
 */
public class MavenITmng4464PlatformIndependentFileSeparatorTest
    extends AbstractMavenIntegrationTestCase
{

    public MavenITmng4464PlatformIndependentFileSeparatorTest()
    {
        super( "[3.0-alpha-7,)" );
    }

    /**
     * Test that Maven recognizes both the forward and the backward slash as file separators, regardless of the
     * underlying filesystem (i.e. even on Unix).
     *
     * @throws Exception in case of failure
     */
    public void testit()
        throws Exception
    {
        File testDir = ResourceExtractor.simpleExtractResources( getClass(), "/mng-4464" );

        Verifier verifier = newVerifier( new File( testDir, "aggregator" ).getAbsolutePath() );
        verifier.setAutoclean( false );
        verifier.deleteDirectory( "../sub/target" );
        verifier.deleteArtifacts( "org.apache.maven.its.mng4464" );
        verifier.executeGoal( "validate" );
        verifier.verifyErrorFreeLog();
        verifier.resetStreams();

        verifier.verifyFilePresent( "../sub/target/path.properties" );
        Properties props = verifier.loadProperties( "../sub/target/path.properties" );
        assertPath( props, "project.build.resources.0.directory", "src/main/res" );
        assertPath( props, "project.build.testResources.0.directory", "src/test/res" );
        assertPath( props, "project.build.sourceDirectory", "src/main/j" );
        assertPath( props, "project.build.testSourceDirectory", "src/test/j" );
        assertPath( props, "project.build.directory", "target/it" );
        assertPath( props, "project.build.outputDirectory", "target/it/classes" );
        assertPath( props, "project.build.testOutputDirectory", "target/it/test-classes" );
    }

    private void assertPath( Properties props, String key, String path )
    {
        String actual = props.getProperty( key, "" );
        assertTrue( actual, actual.endsWith( path.replace( '/', File.separatorChar ) ) );
    }

}
