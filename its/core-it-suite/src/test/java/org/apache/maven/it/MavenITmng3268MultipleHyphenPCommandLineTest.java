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

import org.junit.jupiter.api.Test;

/**
 * This is a test set for <a href="https://issues.apache.org/jira/browse/MNG-3268">MNG-3268</a>.
 *
 *
 */
public class MavenITmng3268MultipleHyphenPCommandLineTest
    extends AbstractMavenIntegrationTestCase
{

    public MavenITmng3268MultipleHyphenPCommandLineTest()
    {
        super( "(2.0.9,)" );
    }

    @Test
    public void testMultipleProfileParams()
        throws Exception
    {
        File testDir = ResourceExtractor.simpleExtractResources( getClass(), "/mng-3268" );

        Verifier verifier;

        verifier = newVerifier( testDir.getAbsolutePath(), "remote" );

        verifier.addCliOption( "-Pprofile1,profile2" );
        verifier.addCliOption( "-Pprofile3" );
        verifier.addCliOption( "-P" );
        verifier.addCliOption( "profile4" );
        verifier.executeGoal( "package" );

        verifier.verifyErrorFreeLog();
        verifier.verifyFilePresent( "target/profile1/touch.txt" );
        verifier.verifyFilePresent( "target/profile2/touch.txt" );
        verifier.verifyFilePresent( "target/profile3/touch.txt" );
        verifier.verifyFilePresent( "target/profile4/touch.txt" );
        verifier.resetStreams();
    }

}
