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
import java.util.Properties;

/**
 * This is a test set for <a href="https://issues.apache.org/jira/browse/MNG-3904">MNG-3904</a>.
 *
 * @author Benjamin Bentmann
 *
 */
public class MavenITmng3904NestedBuildDirInterpolationTest
    extends AbstractMavenIntegrationTestCase
{

    public MavenITmng3904NestedBuildDirInterpolationTest()
    {
        super( "[2.1.0-M1,)" );
    }

    /**
     * Test that properties which refer to build directories which in turn refer to other build directories are
     * properly interpolated.
     */
    public void testitMNG3904()
        throws Exception
    {
        File testDir = ResourceExtractor.simpleExtractResources( getClass(), "/mng-3904" );

        Verifier verifier = newVerifier( testDir.getAbsolutePath() );
        verifier.setAutoclean( false );
        verifier.deleteDirectory( "target" );
        verifier.executeGoal( "validate" );
        verifier.verifyErrorFreeLog();
        verifier.resetStreams();

        Properties props = verifier.loadProperties( "target/pom.properties" );
        assertCanonicalFileEquals( new File( testDir, "target/classes/dir0" ),
            new File( props.getProperty( "project.properties.dir0" ) ) );
        assertCanonicalFileEquals( new File( testDir, "src/test/dir1" ),
            new File( props.getProperty( "project.properties.dir1" ) ) );
        assertCanonicalFileEquals( new File( testDir, "target/site/dir2" ),
            new File( props.getProperty( "project.properties.dir2" ) ) );
    }

}
