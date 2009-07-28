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

import java.io.File;
import java.util.Properties;

import org.apache.maven.it.util.ResourceExtractor;

/**
 * 
 * @author <a href="mailto:oching@apache.org">Maria Odea Ching</a>
 */
public class MavenITmng4189UniqueVersionSnapshotTest
    extends AbstractMavenIntegrationTestCase
{
    public MavenITmng4189UniqueVersionSnapshotTest()
    {
        super( "[2.2.1,)" );
    }

    public void testmng4189()
        throws Exception
    {
        File testDir = ResourceExtractor.simpleExtractResources( getClass(), "/mng-4189" );

        Verifier verifier = new Verifier( testDir.getAbsolutePath() );
        verifier.setAutoclean( false );
        verifier.deleteDirectory( "target" );
        verifier.deleteArtifacts( "org.apache.maven.its.mng4189" );
        Properties filterProps = verifier.newDefaultFilterProperties();
        verifier.filterFile( "settings-template.xml", "settings.xml", "UTF-8", filterProps );
        verifier.getCliOptions().add( "--settings" );
        verifier.getCliOptions().add( "settings.xml" );
        
        // depend on org.apache.maven.its.mng4189:dep:1.0-20090608.090416-1:jar 
        //      which contains add() method
        verifier.executeGoal( "compile" );
        verifier.verifyErrorFreeLog();
       
        verifier.deleteDirectory( "target" );
        verifier.getCliOptions().add( "-f" );
        verifier.getCliOptions().add( "dependent-on-newer-timestamp-pom.xml" );
        try
        {
         // depend on org.apache.maven.its.mng4189:dep:1.0-20090608.090532-2-1:jar 
         //     which DOES NOT contains add() method
            verifier.executeGoal( "compile" );
            fail( "Build should have failed due to compile errors!" );
        }
        catch ( VerificationException e )
        {
            assertTrue( true );
        }
        verifier.verifyTextInLog( "org.apache.maven.plugin.CompilationFailureException: Compilation failure" );
        
        verifier.deleteDirectory( "target" );
        
        // revert back to org.apache.maven.its.mng4189:dep:1.0-20090608.090416-1:jar 
        //      which contains the add() method
        verifier.getCliOptions().remove( "-f" );
        verifier.getCliOptions().remove( "dependent-on-newer-timestamp-pom.xml" );
        verifier.executeGoal( "compile" );
        verifier.verifyErrorFreeLog();
        verifier.resetStreams();

        verifier.deleteArtifacts( "org.apache.maven.its.mng4189" );
    }
}
