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
 * This is a test set for <a href="https://issues.apache.org/jira/browse/MNG-4350">MNG-4350</a>.
 *
 * @author Benjamin Bentmann
 */
public class MavenITmng4350LifecycleMappingExecutionOrderTest
    extends AbstractMavenIntegrationTestCase
{

    public MavenITmng4350LifecycleMappingExecutionOrderTest()
    {
        super( ALL_MAVEN_VERSIONS );
    }

    /**
     * Test that multiple goals bound to the same phase by a lifecycle mapping execute in the order given by
     * the lifecycle mapping. In particular, the order of plugin declarations in the POM should have no influence
     * on the lifecycle mappings specified by the packaging.
     *
     * @throws Exception in case of failure
     */
    @Test
    public void testit()
        throws Exception
    {
        File testDir = ResourceExtractor.simpleExtractResources( getClass(), "/mng-4350" );

        Verifier verifier = newVerifier( testDir.getAbsolutePath() );
        verifier.setAutoclean( false );
        verifier.deleteDirectory( "target" );
        verifier.executeGoal( "process-resources" );
        verifier.verifyErrorFreeLog();

        List<String> lines = verifier.loadLines( "target/log.txt", "UTF-8" );
        assertEquals( Arrays.asList( new String[] { "first", "second" } ), lines );
    }

}
