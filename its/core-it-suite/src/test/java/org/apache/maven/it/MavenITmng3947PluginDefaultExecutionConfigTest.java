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
 * This is a test set for <a href="https://issues.apache.org/jira/browse/MNG-3947">MNG-3947</a>.
 *
 * @author Benjamin Bentmann
 *
 */
public class MavenITmng3947PluginDefaultExecutionConfigTest
    extends AbstractMavenIntegrationTestCase
{

    public MavenITmng3947PluginDefaultExecutionConfigTest()
    {
        super( ALL_MAVEN_VERSIONS );
    }

    /**
     * Test that the configuration for a plugin execution with the identifier "default" does not pollute the
     * configuration of standalone plugin executions from the CLI.
     *
     * @throws Exception in case of failure
     */
    @Test
    public void testitMNG3947()
        throws Exception
    {
        File testDir = ResourceExtractor.simpleExtractResources( getClass(), "/mng-3947" );

        Verifier verifier = newVerifier( testDir.getAbsolutePath() );
        verifier.setAutoclean( false );
        verifier.deleteDirectory( "target" );
        verifier.executeGoal( "org.apache.maven.plugins:maven-resources-plugin:resources" );
        verifier.verifyErrorFreeLog();

        verifier.verifyFileNotPresent( "target/failed.txt" );
        verifier.verifyFilePresent( "target/resources-resources.txt" );
    }

}
