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
 * This is a test set for <a href="https://issues.apache.org/jira/browse/MNG-2871">MNG-2871</a>.
 *
 * @author Benjamin Bentmann
 *
 */
public class MavenITmng2871PrePackageSubartifactResolutionTest
    extends AbstractMavenIntegrationTestCase
{

    public MavenITmng2871PrePackageSubartifactResolutionTest()
    {
        super( "[3.0-alpha-1,)" );
    }

    /**
     * Verify that dependencies on not-yet-packaged sub artifacts in build phases prior to package can be satisfied
     * from a module's output directory, i.e. with the loose class files.
     *
     * @throws Exception in case of failure
     */
    public void testitMNG2871()
        throws Exception
    {
        File testDir = ResourceExtractor.simpleExtractResources( getClass(), "/mng-2871" );
        Verifier verifier = newVerifier( testDir.getAbsolutePath() );
        verifier.setAutoclean( false );
        verifier.deleteDirectory( "consumer/target" );
        verifier.executeGoal( "compile" );
        verifier.verifyErrorFreeLog();
        verifier.resetStreams();

        List<String> compileClassPath = verifier.loadLines( "consumer/target/compile.txt", "UTF-8" );
        assertEquals( 2, compileClassPath.size() );
        assertEquals( new File( testDir, "ejbs/target/classes" ).getCanonicalFile(),
            new File( compileClassPath.get( 1 ).toString() ).getCanonicalFile() );
    }

}
