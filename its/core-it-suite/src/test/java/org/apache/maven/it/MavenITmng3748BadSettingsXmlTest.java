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
import java.util.ArrayList;
import java.util.List;

import org.apache.maven.it.VerificationException;
import org.apache.maven.it.Verifier;
import org.apache.maven.it.util.ResourceExtractor;

/**
 * This is a test set for <a href="http://jira.codehaus.org/browse/MNG-3748">MNG-3748</a>.
 * 
 * Verifies that the settings.xml file is parsed using strict mode, such that invalid
 * xml will cause an error (specifically, when repositories are not contained within a profile declaration)
 *
 * @author jdcasey
 * 
 */
public class MavenITmng3748BadSettingsXmlTest
    extends AbstractMavenIntegrationTestCase
{
    public MavenITmng3748BadSettingsXmlTest()
    {
        super( "(2.0.8,)" ); // only test in 2.0.9+
    }

    public void testitMNG3748 ()
        throws Exception
    {
        File testDir = ResourceExtractor.simpleExtractResources( getClass(), "/mng-3748-badSettingsXml" );

        Verifier verifier;

        verifier = new Verifier( testDir.getAbsolutePath() );

        List cliOptions = new ArrayList();
        cliOptions.add( "-s" );
        cliOptions.add( "settings.xml" );
        
        verifier.setCliOptions( cliOptions );

        try
        {
            verifier.executeGoal( "validate" );
            verifier.verifyErrorFreeLog();
            
            fail( "build should fail if settings.xml doesn't contain valid xml." );
        }
        catch ( VerificationException e )
        {
            // expected
        }
        
        verifier.resetStreams();
    }
}
