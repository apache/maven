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
 * This is a test set for <a href="https://issues.apache.org/jira/browse/MNG-4332">MNG-4332</a>.
 *
 * @author Benjamin Bentmann
 */
public class MavenITmng4332DefaultPluginExecutionOrderTest
    extends AbstractMavenIntegrationTestCase
{

    public MavenITmng4332DefaultPluginExecutionOrderTest()
    {
        super( ALL_MAVEN_VERSIONS );
    }

    /**
     * Verify that default plugin executions contributed by the packaging are executed before user-defined
     * executions from the POM's build section, regardless whether the executions are defined in the regular
     * plugins section or the plugin management section.
     *
     * @throws Exception in case of failure
     */
    @Test
    public void testit()
        throws Exception
    {
        File testDir = ResourceExtractor.simpleExtractResources( getClass(), "/mng-4332" );

        Verifier verifier = newVerifier( testDir.getAbsolutePath() );
        verifier.setAutoclean( false );
        verifier.deleteDirectory( "target" );
        verifier.executeGoal( "process-resources" );
        verifier.verifyErrorFreeLog();

        List<String> lines = verifier.loadLines( "target/resources-resources.txt", "UTF-8" );
        assertEquals( Arrays.asList( new String[] { "default", "test-1", "test-2" } ), lines );
    }

}
