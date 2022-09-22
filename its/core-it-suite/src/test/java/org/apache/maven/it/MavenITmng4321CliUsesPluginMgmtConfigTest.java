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

/**
 * This is a test set for <a href="https://issues.apache.org/jira/browse/MNG-4321">MNG-4321</a>.
 *
 * @author Benjamin Bentmann
 */
public class MavenITmng4321CliUsesPluginMgmtConfigTest
    extends AbstractMavenIntegrationTestCase
{

    public MavenITmng4321CliUsesPluginMgmtConfigTest()
    {
        super( ALL_MAVEN_VERSIONS );
    }

    /**
     * Test that configuration from plugin management also applies to goals that are invoked directly from the
     * CLI even when the invoked plugin is neither explicitly present in the build/plugins section nor part of
     * the lifecycle mappings for the project's packaging.
     *
     * @throws Exception in case of failure
     */
    public void testit()
        throws Exception
    {
        File testDir = ResourceExtractor.simpleExtractResources( getClass(), "/mng-4321" );

        Verifier verifier = newVerifier( testDir.getAbsolutePath() );
        verifier.setAutoclean( false );
        verifier.deleteDirectory( "target" );
        verifier.executeGoal( "org.apache.maven.its.plugins:maven-it-plugin-log-file:2.1-SNAPSHOT:reset" );
        verifier.verifyErrorFreeLog();
        verifier.resetStreams();

        verifier.verifyFilePresent( "target/passed.log" );
    }

}
