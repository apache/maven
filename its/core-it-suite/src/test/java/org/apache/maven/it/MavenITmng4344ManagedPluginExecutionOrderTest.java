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
import java.util.Arrays;
import java.util.List;

/**
 * This is a test set for <a href="https://issues.apache.org/jira/browse/MNG-4344">MNG-4344</a>.
 *
 * @author Benjamin Bentmann
 */
public class MavenITmng4344ManagedPluginExecutionOrderTest
    extends AbstractMavenIntegrationTestCase
{

    public MavenITmng4344ManagedPluginExecutionOrderTest()
    {
        super( ALL_MAVEN_VERSIONS );
    }

    /**
     * Test that custom executions from managed plugins which are part of the default lifecycle bindings get
     * executed after executions from plugins that are defined in the regular build section and bound to the
     * same phase.
     *
     * @throws Exception in case of failure
     */
    public void testit()
        throws Exception
    {
        File testDir = ResourceExtractor.simpleExtractResources( getClass(), "/mng-4344" );

        Verifier verifier = newVerifier( testDir.getAbsolutePath() );
        verifier.setAutoclean( false );
        verifier.deleteDirectory( "target" );
        verifier.executeGoal( "process-resources" );
        verifier.verifyErrorFreeLog();
        verifier.resetStreams();

        List<String> lines = verifier.loadLines( "target/log.txt", "UTF-8" );
        assertEquals( Arrays.asList( new String[] { "first", "second" } ), lines );
    }

}
