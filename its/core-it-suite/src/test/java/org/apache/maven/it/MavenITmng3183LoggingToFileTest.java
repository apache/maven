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
import java.util.Iterator;
import java.util.List;
import java.util.Properties;

/**
 * This is a test set for <a href="http://jira.codehaus.org/browse/MNG-3183">MNG-3183</a>.
 * 
 * @author Benjamin Bentmann
 */
public class MavenITmng3183LoggingToFileTest
    extends AbstractMavenIntegrationTestCase
{

    public MavenITmng3183LoggingToFileTest()
    {
        super( "[3.0-alpha-1,)" );
    }

    /**
     * Test that the CLI parameter -l can be used to direct logging to a file.
     */
    public void testit()
        throws Exception
    {
        File testDir = ResourceExtractor.simpleExtractResources( getClass(), "/mng-3183" );

        Verifier verifier = new Verifier( testDir.getAbsolutePath() );
        verifier.setAutoclean( false );
        verifier.getCliOptions().add( "-l" );
        verifier.getCliOptions().add( "maven.log" );
        verifier.setLogFileName( "stdout.txt" );
        new File( testDir, "stdout.txt" ).delete();
        new File( testDir, "maven.log" ).delete();
        verifier.executeGoal( "validate" );
        verifier.verifyErrorFreeLog();
        verifier.resetStreams();

        List stdout = verifier.loadLines( "stdout.txt", "UTF-8" );

        for ( Iterator it = stdout.iterator(); it.hasNext(); )
        {
            String line = it.next().toString();
            if ( line.startsWith( "+" ) )
            {
                it.remove();
            }
        }

        assertEquals( Collections.EMPTY_LIST, stdout );

        List log = verifier.loadLines( "maven.log", "UTF-8" );

        assertFalse( log.isEmpty() );
    }

}
