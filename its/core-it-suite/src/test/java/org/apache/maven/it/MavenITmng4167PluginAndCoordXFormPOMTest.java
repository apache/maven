/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *  http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */

package org.apache.maven.it;

import org.apache.maven.artifact.versioning.InvalidVersionSpecificationException;
import org.apache.maven.it.util.ResourceExtractor;

import java.io.File;
import java.util.Collections;
import java.util.List;

/**
 * This is a test set for <a href="http://jira.codehaus.org/browse/MNG-4167">MNG-4167</a>.
 *
 * @todo Fill in a better description of what this test verifies!
 * 
 * @author <a href="mailto:brianf@apache.org">Brian Fox</a>
 * @author jdcasey
 * 
 */
public class MavenITmng4167PluginAndCoordXFormPOMTest
    extends AbstractMavenIntegrationTestCase
{
    public MavenITmng4167PluginAndCoordXFormPOMTest()
        throws InvalidVersionSpecificationException
    {
        super( "(2.1.0,)" ); // only test in 2.0.9+
    }

    public void testIt ()
        throws Exception
    {
        File testDir = ResourceExtractor.simpleExtractResources( getClass(), "/mng-4167" );

        Verifier verifier;

        verifier = new Verifier( testDir.getAbsolutePath() );
        verifier.deleteArtifact( "org.apache.maven.its.mng4167", "mng-4167", "1", "pom" );

		String specVersion = System.getProperty( "java.specification.version" );
		
        verifier.deleteArtifact( "org.apache.maven.its.mng4167." + specVersion, "mng-4167-" + specVersion, "1-"
            + specVersion, "pom" );
        
        verifier.setCliOptions( Collections.singletonList( "-X" ) );

        verifier.executeGoal( "install" );
        
        List originalPomLines = verifier.loadFile( new File( testDir, "pom.xml" ), false );
        
        List derivedPomLines = verifier.loadFile( new File( testDir, "target/pom-copy.xml" ), false );
        
        assertFalse( derivedPomLines.equals( originalPomLines ) );
        
        File repoPomPath =
            new File( verifier.getArtifactPath( "org.apache.maven.its.mng4167." + specVersion, "mng-4167-"
                + specVersion, "1-" + specVersion, "pom" ) );
        
        List repoPomLines = verifier.loadFile( repoPomPath, false );
        
        assertEquals( repoPomLines, derivedPomLines );

        verifier.verifyErrorFreeLog();

        verifier.resetStreams();
    }
}
