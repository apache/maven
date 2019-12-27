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
import org.apache.maven.it.Verifier;

import java.io.File;

/**
 * This is a test set for <a href="https://issues.apache.org/jira/browse/MNG-3536">MNG-3536</a>.
 * 
 *
 */
public class MavenITmng3536AppendedAbsolutePathsTest
    extends AbstractMavenIntegrationTestCase
{
    
    public MavenITmng3536AppendedAbsolutePathsTest()
    {
        super( "[2.1.0-M1,)"); // 2.1.0+ only
    }

    public void testitMNG3536()
        throws Exception
    {
        File testDir = ResourceExtractor.simpleExtractResources( getClass(),
                                                                 "/mng-3536" );
        File pluginDir = new File( testDir, "plugin" );
        Verifier verifier = newVerifier( pluginDir.getAbsolutePath(), "remote" );

        verifier.executeGoal( "install" );

        verifier.verifyErrorFreeLog();
        verifier.resetStreams();

        File projectDir = new File( testDir, "project" );
        verifier = newVerifier( projectDir.getAbsolutePath() );

        verifier.executeGoal( "verify" );
        verifier.verifyErrorFreeLog();
        verifier.resetStreams();
    }
}
