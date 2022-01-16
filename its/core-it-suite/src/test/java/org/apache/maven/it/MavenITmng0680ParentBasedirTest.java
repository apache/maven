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
import java.util.Properties;

/**
 * This is a test set for <a href="https://issues.apache.org/jira/browse/MNG-680">MNG-680</a>.
 *
 * @author Brett Porter
 *
 */
public class MavenITmng0680ParentBasedirTest
    extends AbstractMavenIntegrationTestCase
{
    public MavenITmng0680ParentBasedirTest()
    {
        super( ALL_MAVEN_VERSIONS );
    }

    /**
     * Test that the basedir of the parent is set correctly.
     *
     * @throws Exception in case of failure
     */
    public void testitMNG680()
        throws Exception
    {
        File testDir = ResourceExtractor.simpleExtractResources( getClass(), "/mng-0680" );

        testDir = testDir.getCanonicalFile();

        File subDir = new File( testDir, "subproject" );

        Verifier verifier = newVerifier( subDir.getAbsolutePath() );
        verifier.setAutoclean( false );
        verifier.deleteDirectory( "target" );
        verifier.executeGoal( "validate" );
        verifier.verifyErrorFreeLog();
        verifier.resetStreams();

        Properties props = verifier.loadProperties( "target/basedir.properties" );
        assertEquals( subDir, new File( props.getProperty( "project.basedir" ) ) );
        assertEquals( testDir, new File( props.getProperty( "project.parent.basedir" ) ) );
    }

}
