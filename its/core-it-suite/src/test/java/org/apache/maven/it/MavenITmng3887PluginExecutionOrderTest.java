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
 * This is a test set for <a href="https://issues.apache.org/jira/browse/MNG-3887">MNG-3887</a>.
 *
 * @author Benjamin Bentmann
 *
 */
public class MavenITmng3887PluginExecutionOrderTest
    extends AbstractMavenIntegrationTestCase
{

    public MavenITmng3887PluginExecutionOrderTest()
    {
        super( ALL_MAVEN_VERSIONS );
    }

    /**
     * Test that multiple plugin executions bound to the same phase are executed in the order given by the POM when no
     * {@code <pluginManagement>} is involved.
     *
     * @throws Exception in case of failure
     */
    @Test
    public void testitWithoutPluginMngt()
        throws Exception
    {
        testitMNG3887( "test-1" );
    }

    /**
     * Test that multiple plugin executions bound to the same phase are executed in the order given by the POM when
     * {@code <pluginManagement>} is involved.
     *
     * @throws Exception in case of failure
     */
    @Test
    public void testitWithPluginMngt()
        throws Exception
    {
        testitMNG3887( "test-2" );
    }

    private void testitMNG3887( String project )
        throws Exception
    {
        File testDir = ResourceExtractor.simpleExtractResources( getClass(), "/mng-3887" );

        Verifier verifier = newVerifier( new File( testDir, project ).getAbsolutePath() );
        verifier.setAutoclean( false );
        verifier.deleteDirectory( "target" );
        verifier.addCliArgument( "validate" );
        verifier.execute();
        verifier.verifyErrorFreeLog();

        List<String> lines = verifier.loadLines( "target/it.log", "UTF-8" );
        assertEquals( Arrays.asList( new String[] { "test", "----" } ), lines );
    }

}
