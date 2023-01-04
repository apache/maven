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
import java.util.Arrays;
import java.util.List;

import org.junit.jupiter.api.Test;

/**
 * This is a test set for <a href="https://issues.apache.org/jira/browse/MNG-4975">MNG-4975</a>.
 *
 * @author Benjamin Bentmann
 */
public class MavenITmng4975ProfileInjectedPluginExecutionOrderTest
    extends AbstractMavenIntegrationTestCase
{

    public MavenITmng4975ProfileInjectedPluginExecutionOrderTest()
    {
        super( "[2.0.7,3.0-alpha-1),[3.0.3,)" );
    }

    /**
     * Test that plugin executions (in the same phase) are properly ordered after profile injection.
     *
     * @throws Exception in case of failure
     */
    @Test
    public void testit()
        throws Exception
    {
        File testDir = ResourceExtractor.simpleExtractResources( getClass(), "/mng-4975" );

        Verifier verifier = newVerifier( testDir.getAbsolutePath() );
        verifier.setAutoclean( false );
        verifier.deleteDirectory( "target" );
        verifier.addCliOption( "-Pprofile2,profile1" );
        verifier.executeGoal( "validate" );
        verifier.verifyErrorFreeLog();

        List<String> lines = verifier.loadLines( "target/exec.log", "UTF-8" );
        List<String> expected = Arrays.asList( new String[] { "1", "2", "3", "4", "5" } );
        assertEquals( expected, lines );
    }

}
