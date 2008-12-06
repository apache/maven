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
import java.util.List;

/**
 * This is a test set for <a href="http://jira.codehaus.org/browse/MNG-95">MNG-95</a>.
 * 
 * @author John Casey
 * @version $Id$
 */
public class MavenITmng0095ReactorFailureBehaviorTest
    extends AbstractMavenIntegrationTestCase
{

    /**
     * Test fail-never reactor behavior. Forces an exception to be thrown in
     * the first module, but checks that the second modules is built.
     */
    public void testitMNG95()
        throws Exception
    {
        File testDir = ResourceExtractor.simpleExtractResources( getClass(), "/mng-0095" );
        Verifier verifier = new Verifier( testDir.getAbsolutePath() );
        verifier.deleteArtifact( "org.apache.maven.plugins", "maven-it-it-plugin", "1.0", "maven-plugin" );
        List cliOptions = new ArrayList();
        cliOptions.add( "--no-plugin-registry --fail-never" );
        verifier.setCliOptions( cliOptions );
        verifier.executeGoal( "org.apache.maven.its.plugins:maven-it-plugin-touch:touch" );
        verifier.assertFilePresent( "target/touch.txt" );
        verifier.assertFileNotPresent( "subproject/target/touch.txt" );
        verifier.assertFilePresent( "subproject2/target/touch.txt" );
        verifier.verifyErrorFreeLog();
        verifier.resetStreams();

    }
}

