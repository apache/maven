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
import java.util.Properties;

/**
 * This is a test set for <a href="http://jira.codehaus.org/browse/MNG-2097">MNG-2097</a>.
 * 
 * @author Brett Porter
 * @version $Id: MavenITmng3906MergedPluginClassPathOrderingTest.java 726432 2008-12-14 13:20:02Z bentmann $
 */
public class MavenITmng2097PreparePackagePhaseTest
    extends AbstractMavenIntegrationTestCase
{

    public MavenITmng2097PreparePackagePhaseTest()
    {
        super( "(2.1.0-M1,)" ); // Maven 2.1.0-M2 +
    }

    /**
     * Test that the prepare package phase can be bound to and executed.
     */
    public void testPreparePackagePhase()
        throws Exception
    {
        File testDir = ResourceExtractor.simpleExtractResources( getClass(), "/mng-2097" );

        Verifier verifier = new Verifier( testDir.getAbsolutePath() );
        verifier.executeGoal( "prepare-package" );
        verifier.assertFilePresent( "target/touch.txt" );
        verifier.verifyErrorFreeLog();
        verifier.resetStreams();
        
        new File( testDir, "target/touch.txt" ).delete();
        verifier.assertFileNotPresent( "target/touch.txt" );
        
        verifier.executeGoal( "package" );
        verifier.assertFilePresent( "target/touch.txt" );
        verifier.verifyErrorFreeLog();
        verifier.resetStreams();
        
        new File( testDir, "target/touch.txt" ).delete();
        verifier.assertFileNotPresent( "target/touch.txt" );
        
        verifier.executeGoal( "test" );
        verifier.assertFileNotPresent( "target/touch.txt" );
        verifier.verifyErrorFreeLog();
        verifier.resetStreams();
    }

}
