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

/**
 * This is a test set for <a href="https://issues.apache.org/jira/browse/MNG-3581">MNG-3581</a>.
 * 
 *
 */
public class MavenITmng3581PluginUsesWagonDependencyTest
    extends AbstractMavenIntegrationTestCase
{

    public MavenITmng3581PluginUsesWagonDependencyTest()
    {
        // Not 2.0.9
        super( "(2.0.4,2.0.9),(2.0.9,)" );
    }

    /**
     * Test that a plugin using a specific wagon implementation directly works.
     */
    public void testit()
        throws Exception
    {
        File testDir = ResourceExtractor.simpleExtractResources( getClass(), "/mng-3581" );

        Verifier verifier = newVerifier( testDir.getAbsolutePath() );
        verifier.setAutoclean( false );
        verifier.executeGoal( "initialize" );
        verifier.verifyErrorFreeLog();
        verifier.resetStreams();
    }

}
