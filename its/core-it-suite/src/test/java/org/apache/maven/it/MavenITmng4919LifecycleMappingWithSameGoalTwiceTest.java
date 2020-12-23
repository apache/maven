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
import java.util.List;

/**
 * This is a test set for <a href="https://issues.apache.org/jira/browse/MNG-4919">MNG-4919</a>.
 *
 * @author Benjamin Bentmann
 */
public class MavenITmng4919LifecycleMappingWithSameGoalTwiceTest
    extends AbstractMavenIntegrationTestCase
{

    public MavenITmng4919LifecycleMappingWithSameGoalTwiceTest()
    {
        super( "[2.0.3,3.0-alpha-1),[3.0.2,)" );
    }

    /**
     * Verify that lifecycle mappings can bind a goal twice, say in different phases.
     */
    public void testit()
        throws Exception
    {
        File testDir = ResourceExtractor.simpleExtractResources( getClass(), "/mng-4919" );

        Verifier verifier = newVerifier( testDir.getAbsolutePath() );
        verifier.setAutoclean( false );
        verifier.deleteDirectory( "target" );
        verifier.executeGoals( Arrays.asList( new String[] { "clean", "validate" } ) );
        verifier.verifyErrorFreeLog();
        verifier.resetStreams();

        List<String> lines = verifier.loadLines( "target/log.txt", "UTF-8" );
        assertEquals( Arrays.asList( new String[] { "check", "check" } ), lines );
    }

}
