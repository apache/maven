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
import java.util.List;

/**
 * This is a test set for <a href="https://issues.apache.org/jira/browse/MNG-4913">MNG-4913</a>.
 *
 * @author Benjamin Bentmann
 */
public class MavenITmng4913UserPropertyVsDependencyPomPropertyTest
    extends AbstractMavenIntegrationTestCase
{

    public MavenITmng4913UserPropertyVsDependencyPomPropertyTest()
    {
        super( "[2.0.9,3.0-alpha-1),[3.0.2,)" );
    }

    /**
     * Verify that user properties from the CLI do not override POM properties of transitive dependencies.
     *
     * @throws Exception in case of failure
     */
    public void testit()
        throws Exception
    {
        File testDir = ResourceExtractor.simpleExtractResources( getClass(), "/mng-4913" );

        Verifier verifier = newVerifier( testDir.getAbsolutePath() );
        verifier.setAutoclean( false );
        verifier.deleteDirectory( "target" );
        verifier.deleteArtifacts( "org.apache.maven.its.mng4913" );
        verifier.setSystemProperty( "mng4913.version", "98.76" );
        verifier.addCliOption( "-s" );
        verifier.addCliOption( "settings.xml" );
        verifier.filterFile( "settings-template.xml", "settings.xml", "UTF-8", verifier.newDefaultFilterProperties() );
        verifier.executeGoal( "validate" );
        verifier.verifyErrorFreeLog();
        verifier.resetStreams();

        List<String> classpath = verifier.loadLines( "target/classpath.txt", "UTF-8" );

        assertTrue( classpath.toString(), classpath.contains( "a-0.1.jar" ) );
        assertTrue( classpath.toString(), classpath.contains( "b-0.1.jar" ) );
        assertFalse( classpath.toString(), classpath.contains( "a-98.76.jar" ) );
    }

}
