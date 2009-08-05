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

import org.apache.maven.it.util.ResourceExtractor;

import java.io.File;
import java.util.Iterator;
import java.util.List;

/**
 * This is a test set for <a href="http://jira.codehaus.org/browse/MNG-4275">MNG-4275</a>.
 *
 * @author John Casey
 */
public class MavenITmng4275RelocationWarningTest
    extends AbstractMavenIntegrationTestCase
{

    public MavenITmng4275RelocationWarningTest()
    {
        super( "[2.0,2.0.9)(2.2.0,2.99.99]" );
    }

    public void testit()
        throws Exception
    {
        File testDir = ResourceExtractor.simpleExtractResources( getClass(), "/mng-4275" );
        File depsDir = new File( testDir, "dependencies" );
        File projectDir = new File( testDir, "project" );

        Verifier verifier = new Verifier( depsDir.getAbsolutePath() );
        
        verifier.deleteArtifacts( "org.apache.maven.it.mng4275" );
        
        verifier.executeGoal( "install" );
        
        verifier.verifyErrorFreeLog();
        verifier.resetStreams();
        
        // now, build the project that depends on the above.
        verifier = new Verifier( projectDir.getAbsolutePath() );
        
        verifier.executeGoal( "install" );
        
        verifier.verifyErrorFreeLog();
        verifier.resetStreams();
        
        List lines = verifier.loadFile( new File( projectDir, verifier.getLogFileName() ), false );
        boolean foundWarning = false;
        for ( Iterator it = lines.iterator(); it.hasNext(); )
        {
            String line = (String) it.next();
            
            if ( foundWarning )
            {
                assertTrue(
                            "Relocation target should have been logged right after warning.",
                            line.indexOf( "This artifact has been relocated to org.apache.maven.it.mng4275:relocated:1" ) > -1 );
                break;
            }
            else if ( line.startsWith( "[WARNING] While downloading org.apache.maven.it.mng4275:relocation:1" ) )
            {
                foundWarning = true;
            }
        }
        
        assertTrue( "Relocation warning should haven been logged.", foundWarning );
    }

}
