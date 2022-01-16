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
import java.util.List;
import java.util.Properties;

/**
 * This is a test set for <a href="https://issues.apache.org/jira/browse/MNG-3297">MNG-3297</a>.
 *
 * @author Benjamin Bentmann
 */
public class MavenITmng3297DependenciesNotLeakedToMojoTest
    extends AbstractMavenIntegrationTestCase
{

    public MavenITmng3297DependenciesNotLeakedToMojoTest()
    {
        super( "[3.0-alpha-7,)" );
    }

    /**
     * Test that project dependencies resolved for one mojo are not exposed to another mojo if the latter
     * does not require dependency resolution.
     *
     * @throws Exception in case of failure
     */
    public void testit()
        throws Exception
    {
        File testDir = ResourceExtractor.simpleExtractResources( getClass(), "/mng-3297" );

        Verifier verifier = newVerifier( testDir.getAbsolutePath() );
        verifier.setAutoclean( false );
        verifier.deleteDirectory( "target" );
        verifier.executeGoal( "initialize" );
        verifier.verifyErrorFreeLog();
        verifier.resetStreams();

        List<String> artifacts = verifier.loadLines( "target/artifacts.txt", "UTF-8" );
        assertEquals( artifacts.toString(), 1, artifacts.size() );

        Properties props = verifier.loadProperties( "target/artifact.properties" );
        assertEquals( "0", props.getProperty( "project.artifacts" ) );
    }

}
