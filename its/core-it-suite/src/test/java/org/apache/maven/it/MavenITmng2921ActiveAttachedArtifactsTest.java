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
import java.util.Collection;

/**
 * This is a test set for <a href="http://jira.codehaus.org/browse/MNG-2921">MNG-2921</a>.
 * 
 * @author Benjamin Bentmann
 * @version $Id$
 */
public class MavenITmng2921ActiveAttachedArtifactsTest
    extends AbstractMavenIntegrationTestCase
{

    public MavenITmng2921ActiveAttachedArtifactsTest()
    {
        super( "(2.0.6,)" );
    }

    /**
     * Verify that attached project artifacts can be resolved from the reactor as active project artifacts for
     * consumption on other module's class paths. Note the subtle difference of this test compared to the closely
     * related issue MNG-2871: This test is about *attached* artifacts, i.e. dependencies that have already been
     * packaged. MNG-2871 on the other hand is about dependencies that haven't been packaged yet but merely exist
     * as loose class files in a module's output directory.
     */
    public void testitMNG2921()
        throws Exception
    {
        File testDir = ResourceExtractor.simpleExtractResources( getClass(), "/mng-2921" );
        Verifier verifier = new Verifier( testDir.getAbsolutePath() );
        verifier.setAutoclean( false );
        verifier.deleteDirectory( "consumer/target" );
        verifier.executeGoal( "validate" );
        verifier.verifyErrorFreeLog();
        verifier.resetStreams();

        Collection compileArtifacts = verifier.loadLines( "consumer/target/compile.txt", "UTF-8" );
        assertTrue( compileArtifacts.toString(),
            compileArtifacts.contains( "org.apache.maven.its.mng2921:ejbs:ejb-client:client:1.0-SNAPSHOT" ) );
        assertFalse( compileArtifacts.toString(),
            compileArtifacts.contains( "org.apache.maven.its.mng2921:tests:test-jar:tests:1.0-SNAPSHOT" ) );

        Collection testArtifacts = verifier.loadLines( "consumer/target/test.txt", "UTF-8" );
        assertTrue( testArtifacts.toString(),
            testArtifacts.contains( "org.apache.maven.its.mng2921:ejbs:ejb-client:client:1.0-SNAPSHOT" ) );
        assertTrue( testArtifacts.toString(),
            testArtifacts.contains( "org.apache.maven.its.mng2921:tests:test-jar:tests:1.0-SNAPSHOT" ) );
    }

}
