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
import java.util.List;

/**
 * This is a test set for <a href="https://issues.apache.org/jira/browse/MNG-1701">MNG-1701</a>.
 *
 * @author Benjamin Bentmann
 */
public class MavenITmng1701DuplicatePluginTest
    extends AbstractMavenIntegrationTestCase
{

    public MavenITmng1701DuplicatePluginTest()
    {
        super( "[3.0-beta-1,)" );
    }

    /**
     * Verify that duplicate plugin declarations cause a warning.
     */
    public void testit()
        throws Exception
    {
        File testDir = ResourceExtractor.simpleExtractResources( getClass(), "/mng-1701" );

        Verifier verifier = newVerifier( testDir.getAbsolutePath() );
        verifier.setAutoclean( false );
        verifier.executeGoal( "validate" );
        verifier.verifyErrorFreeLog();
        verifier.resetStreams();

        List<String> lines = verifier.loadLines( verifier.getLogFileName(), "UTF-8" );
        boolean foundWarning = false;
        for ( String line : lines )
        {
            if ( line.startsWith( "[WARNING]" )
                && line.indexOf( "duplicate declaration of plugin org.apache.maven.its.plugins:maven-it-plugin-expression" ) > 0 )
            {
                foundWarning = true;
            }
        }

        assertTrue( "Duplicate plugin warning wasn't generated.", foundWarning );
    }

}
