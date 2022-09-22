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
import java.util.Collections;
import java.util.List;

/**
 * This is a test set for <a href="https://issues.apache.org/jira/browse/MNG-3916">MNG-3916</a>.
 *
 * @author Benjamin Bentmann
 *
 */
public class MavenITmng3916PluginExecutionInheritanceTest
    extends AbstractMavenIntegrationTestCase
{

    public MavenITmng3916PluginExecutionInheritanceTest()
    {
        super( "(2.0.4,)" );
    }

    /**
     * Test that plugin executions are properly merged during inheritance, even if the child plugin section has no
     * version.
     *
     * @throws Exception in case of failure
     */
    public void testitMNG3916()
        throws Exception
    {
        File testDir = ResourceExtractor.simpleExtractResources( getClass(), "/mng-3916" );

        Verifier verifier = newVerifier( new File( testDir, "sub" ).getAbsolutePath() );
        verifier.setAutoclean( false );
        verifier.deleteDirectory( "target" );
        verifier.executeGoal( "validate" );
        verifier.verifyErrorFreeLog();
        verifier.resetStreams();

        List<String> executions = verifier.loadLines( "target/exec.log", "UTF-8" );
        // NOTE: Ordering of executions is another issue (MNG-3887), so ignore/normalize order
        Collections.sort( executions );
        List<String> expected = Arrays.asList( new String[] { "child-1", "child-default", "parent-1" } );
        assertEquals( expected, executions );
    }

}
