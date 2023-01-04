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

import org.apache.maven.shared.verifier.util.ResourceExtractor;
import org.apache.maven.shared.verifier.Verifier;

import java.io.File;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;

import org.junit.jupiter.api.Test;

/**
 * This is a test set for <a href="https://issues.apache.org/jira/browse/MNG-4387">MNG-4387</a>.
 *
 * @author Benjamin Bentmann
 */
public class MavenITmng4387QuietLoggingTest
    extends AbstractMavenIntegrationTestCase
{

    public MavenITmng4387QuietLoggingTest()
    {
        super( "[2.0.5,)" );
    }

    /**
     * Test that the CLI flag -q enables quiet logging, i.e. suppresses log levels below ERROR.
     *
     * @throws Exception in case of failure
     */
    @Test
    public void testit()
        throws Exception
    {
        File testDir = ResourceExtractor.simpleExtractResources( getClass(), "/mng-4387" );

        Verifier verifier = newVerifier( testDir.getAbsolutePath() );
        verifier.setAutoclean( false );
        verifier.addCliArgument( "-q" );
        verifier.setLogFileName( "log.txt" );
        verifier.addCliArgument( "validate" );
        verifier.execute();
        verifier.verifyErrorFreeLog();

        List<String> lines = verifier.loadLines( "log.txt", "UTF-8" );

        for ( Iterator<String> it = lines.iterator(); it.hasNext(); )
        {
            String line = it.next();
            if ( line.startsWith( "+" ) || line.startsWith( "EMMA" ) )
            {
                it.remove();
            }
        }

        assertEquals( Collections.EMPTY_LIST, lines );
    }

}
