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

/**
 * This is a test set for <a href="https://issues.apache.org/jira/browse/MNG-5338">MNG-5338</a>.
 *
 * @author Olivier Lamy
 */
public class MavenITmng5338FileOptionToDirectory
    extends AbstractMavenIntegrationTestCase
{

    private File testDir;

    public MavenITmng5338FileOptionToDirectory()
    {
        super( "[3.1-A,)" );
    }

    public void setUp()
        throws Exception
    {
        super.setUp();

        testDir = ResourceExtractor.simpleExtractResources( getClass(), "/mng-5338" );

    }

    protected void tearDown()
        throws Exception
    {

        super.tearDown();
    }

    public void testFileOptionToADirectory()
        throws Exception
    {
        Verifier verifier = newVerifier( testDir.getAbsolutePath() );

        verifier.setAutoclean( false );
        verifier.deleteDirectory( "target" );
        verifier.deleteArtifacts( "org.apache.maven.its.mng5338" );

        verifier.addCliOption( "-f" );
        verifier.addCliOption( "project" );
        verifier.executeGoal( "validate" );
        verifier.verifyErrorFreeLog();
        verifier.resetStreams();

    }

}
