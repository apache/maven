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
 * This is a test set for <a href="https://issues.apache.org/jira/browse/MNG-4207">MNG-4207</a>.
 *
 * @author John Casey
 */
public class MavenITmng4207PluginWithLog4JTest
    extends AbstractMavenIntegrationTestCase
{

    public MavenITmng4207PluginWithLog4JTest()
    {
        super( "[2.0.3,)" );
    }

    /**
     * Test that a plugin that depends on log4j and employs the artifact resolver does not die when using
     * commons-http to resolve an artifact.
     *
     * @throws Exception in case of failure
     */
    @Test
    public void testit()
        throws Exception
    {
        File testDir = ResourceExtractor.simpleExtractResources( getClass(), "/mng-4207" );

        Verifier verifier = newVerifier( testDir.getAbsolutePath(), "remote" );
        verifier.setAutoclean( false );
        verifier.deleteArtifacts( "org.apache.maven.its.mng4207" );
        verifier.addCliOption( "-s" );
        verifier.addCliOption( "settings.xml" );
        verifier.executeGoal( "initialize" );
        verifier.verifyErrorFreeLog();
        verifier.resetStreams();
    }

}
