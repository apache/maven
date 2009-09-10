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
import org.apache.maven.it.util.FileUtils;
import org.apache.maven.it.util.ResourceExtractor;

import java.io.File;

/**
 * This is a test set for <a href="http://jira.codehaus.org/browse/MNG-4347">MNG-4347</a>.
 * 
 * @author John Casey
 */
public class MavenITmng4347ImportScopeWithSettingsProfilesTest
    extends AbstractMavenIntegrationTestCase
{

    public MavenITmng4347ImportScopeWithSettingsProfilesTest()
    {
        super( "(2.2.1,]" );
    }

    /**
     * Test that profiles from settings.xml will be used to resolve import-scoped dependency POMs.
     */
    public void testit()
        throws Exception
    {
        File testDir = ResourceExtractor.simpleExtractResources( getClass(), "/mng-4347" );

        Verifier verifier = new Verifier( testDir.getAbsolutePath() );
        
        String localRepo = verifier.localRepo;
        File dest = new File( localRepo );
        File src = new File( testDir, "local-repository" );
        
        verifier.deleteDirectory( "target" );
        verifier.deleteArtifacts( "org.apache.maven.it.mng4347" );
        
        FileUtils.copyDirectoryStructure( src, dest );
        
        verifier.setAutoclean( false );
        
        verifier.getCliOptions().add( "-V" );
        verifier.getCliOptions().add( "-X" );
        verifier.getCliOptions().add( "-s" );
        verifier.getCliOptions().add( "settings.xml" );
        
        verifier.filterFile( "settings-template.xml", "settings.xml", "UTF-8", verifier.newDefaultFilterProperties() );
        
        verifier.executeGoal( "validate" );
        
        verifier.verifyErrorFreeLog();
        verifier.resetStreams();
    }

}
