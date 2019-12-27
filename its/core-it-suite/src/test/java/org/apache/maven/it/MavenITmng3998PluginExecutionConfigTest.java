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
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

/**
 * This is a test set for <a href="https://issues.apache.org/jira/browse/MNG-3998">MNG-3998</a>.
 * 
 * @author Benjamin Bentmann
 *
 */
public class MavenITmng3998PluginExecutionConfigTest
    extends AbstractMavenIntegrationTestCase
{

    public MavenITmng3998PluginExecutionConfigTest()
    {
        super( ALL_MAVEN_VERSIONS );
    }

    /**
     * Test that multiple plugin executions do not lose their configuration when plugin management is used.
     */
    public void testitMNG3998()
        throws Exception
    {
        File testDir = ResourceExtractor.simpleExtractResources( getClass(), "/mng-3998" );

        Verifier verifier = newVerifier( testDir.getAbsolutePath() );
        verifier.setAutoclean( false );
        verifier.deleteDirectory( "target" );
        verifier.executeGoal( "validate" );
        verifier.verifyErrorFreeLog();
        verifier.resetStreams();

        List<String> executions = verifier.loadLines( "target/exec.log", "UTF-8" );
        // NOTE: Ordering of executions is another issue (MNG-3887), so ignore/normalize order
        Collections.sort( executions );
        List<String> expected = Arrays.asList( new String[] { "exec-1", "exec-2" } );
        assertEquals( expected, executions );
    }

}
