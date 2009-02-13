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
import java.util.Collections;
import java.util.List;

/**
 * This is a test set for <a href="http://jira.codehaus.org/browse/MNG-4034">MNG-4034</a>.
 * 
 * @author Benjamin Bentmann
 * @version $Id$
 */
public class MavenITmng4034ManagedProfileDependencyTest
    extends AbstractMavenIntegrationTestCase
{

    public MavenITmng4034ManagedProfileDependencyTest()
    {
        super( "[2.0,)" );
    }

    /**
     * Verify that dependencies defined in profiles get their version injected from the dependency management of the
     * parent.
     */
    public void testitMNG4034()
        throws Exception
    {
        File testDir = ResourceExtractor.simpleExtractResources( getClass(), "/mng-4034" );

        Verifier verifier = new Verifier( new File( testDir, "sub" ).getAbsolutePath() );
        verifier.setAutoclean( false );
        verifier.deleteDirectory( "target" );
        verifier.executeGoal( "validate" );
        verifier.verifyErrorFreeLog();
        verifier.resetStreams();

        List artifacts = verifier.loadLines( "target/artifacts.txt", "UTF-8" );
        assertEquals( Collections.singletonList( "org.apache.maven.its:maven-core-it-support:jar:1.0" ), artifacts );
    }

}
