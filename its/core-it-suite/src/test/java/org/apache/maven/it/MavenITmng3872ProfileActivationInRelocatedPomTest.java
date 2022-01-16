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
 * This is a test set for <a href="https://issues.apache.org/jira/browse/MNG-3872">MNG-3872</a>.
 *
 * @author Benjamin Bentmann
 */
public class MavenITmng3872ProfileActivationInRelocatedPomTest
    extends AbstractMavenIntegrationTestCase
{

    public MavenITmng3872ProfileActivationInRelocatedPomTest()
    {
        super( ALL_MAVEN_VERSIONS );
    }

    /**
     * Verify that profiles are activated in relocated POMs.
     *
     * @throws Exception in case of failure
     */
    public void testit()
        throws Exception
    {
        File testDir = ResourceExtractor.simpleExtractResources( getClass(), "/mng-3872" );

        Verifier verifier = newVerifier( testDir.getAbsolutePath() );
        verifier.setAutoclean( false );
        verifier.deleteDirectory( "target" );
        verifier.deleteArtifacts( "org.apache.maven.its.mng3872" );
        Properties filterProps = verifier.newDefaultFilterProperties();
        verifier.filterFile( "settings-template.xml", "settings.xml", "UTF-8", filterProps );
        verifier.addCliOption( "--settings" );
        verifier.addCliOption( "settings.xml" );
        verifier.executeGoal( "validate" );
        verifier.verifyErrorFreeLog();
        verifier.resetStreams();

        List<String> compileClassPath = verifier.loadLines( "target/compile.txt", "UTF-8" );
        assertTrue( compileClassPath.toString(), compileClassPath.contains( "a-0.1.jar" ) );
        assertTrue( compileClassPath.toString(), compileClassPath.contains( "b-0.1.jar" ) );
        assertFalse( compileClassPath.toString(), compileClassPath.contains( "c-0.1.jar" ) );
    }

}
