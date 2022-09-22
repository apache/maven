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
 * This is a test set for <a href="https://issues.apache.org/jira/browse/MNG-4786">MNG-4786</a>.
 *
 * @author Benjamin Bentmann
 */
public class MavenITmng4786AntBased21xMojoSupportTest
    extends AbstractMavenIntegrationTestCase
{

    public MavenITmng4786AntBased21xMojoSupportTest()
    {
        super( "[2.0.3,3.0-alpha-1),[3.0-beta-4,)" );
    }

    /**
     * Verify that plugins whose mojos are implemented as Ant scripts and use the Maven 2.1.x Ant support can be
     * invoked. The essential bits here are that Ant-based mojos are instantiated via a custom component factory, yet
     * must undergo the same IoC as for regular Java components. And the 2.1.x Ant support actually requires injection
     * of a logger.
     *
     * @throws Exception in case of failure
     */
    public void testit()
        throws Exception
    {
        File testDir = ResourceExtractor.simpleExtractResources( getClass(), "/mng-4786" );

        Verifier verifier = newVerifier( testDir.getAbsolutePath() );
        verifier.setAutoclean( false );
        verifier.deleteDirectory( "target" );
        verifier.executeGoal( "validate" );
        verifier.verifyErrorFreeLog();
        verifier.resetStreams();

        verifier.verifyFilePresent( "target/ant.txt" );
    }

}
