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
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * 
 * @author Benjamin Bentmann
 * @version $Id$
 */
public class MavenIT0144LifecycleExecutionOrderTest
    extends AbstractMavenIntegrationTestCase
{

    public MavenIT0144LifecycleExecutionOrderTest()
    {
        super( "(2.0.4,)" );
    }

    /**
     * Test that the lifecycle phases execute in proper order.
     */
    public void testit0144()
        throws Exception
    {
        File testDir = ResourceExtractor.simpleExtractResources( getClass(), "/it0144" );

        Verifier verifier = new Verifier( testDir.getAbsolutePath() );
        verifier.deleteDirectory( "target" );
        verifier.setAutoclean( false );
        verifier.executeGoals( Arrays.asList( new String[] { "post-clean", "deploy", "site-deploy" } ) );
        verifier.verifyErrorFreeLog();
        verifier.resetStreams();

        List expected = new ArrayList();

        expected.add( "pre-clean" );
        expected.add( "clean" );
        expected.add( "post-clean" );

        expected.add( "validate" );
        expected.add( "initialize" );
        expected.add( "generate-sources" );
        expected.add( "process-sources" );
        expected.add( "generate-resources" );
        expected.add( "process-resources" );
        expected.add( "compile" );
        expected.add( "process-classes" );
        expected.add( "generate-test-sources" );
        expected.add( "process-test-sources" );
        expected.add( "generate-test-resources" );
        expected.add( "process-test-resources" );
        expected.add( "test-compile" );
        expected.add( "process-test-classes" );
        expected.add( "test" );
        expected.add( "package" );
        expected.add( "pre-integration-test" );
        expected.add( "integration-test" );
        expected.add( "post-integration-test" );
        expected.add( "verify" );
        expected.add( "install" );
        expected.add( "deploy" );

        expected.add( "pre-site" );
        expected.add( "site" );
        expected.add( "post-site" );
        expected.add( "site-deploy" );

        List phases = verifier.loadLines( "target/phases.log", "UTF-8" );
        assertEquals( expected, phases );
    }

}
