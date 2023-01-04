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
import java.util.Properties;

import org.junit.jupiter.api.Test;

/**
 * This is a test set for <a href="https://issues.apache.org/jira/browse/MNG-2254">MNG-2254</a>:
 * it tests that pom.xml encoding is properly detected.
 *
 * @author <a href="mailto:herve.boutemy@free.fr">Hervé Boutemy</a>
 *
 */
public class MavenITmng2254PomEncodingTest
    extends AbstractMavenIntegrationTestCase
{

    public MavenITmng2254PomEncodingTest()
    {
        super( "(2.0.7,)" ); // 2.0.8+
    }

    /**
     * Verify that the encoding declaration of the POM is respected.
     *
     * @throws Exception in case of failure
     */
    @Test
    public void testitMNG2254 ()
        throws Exception
    {
        File testDir = ResourceExtractor.simpleExtractResources( getClass(), "/mng-2254" );

        Verifier verifier = newVerifier( testDir.getAbsolutePath() );
        verifier.setAutoclean( false );
        verifier.deleteDirectory( "utf-8/target" );
        verifier.deleteDirectory( "latin-1/target" );
        verifier.addCliArgument( "validate" );
        verifier.execute();
        verifier.verifyErrorFreeLog();

        Properties utf8 = verifier.loadProperties( "utf-8/target/pom.properties" );
        assertEquals( "TEST-CHARS: \u00DF\u0131\u03A3\u042F\u05D0\u20AC", utf8.getProperty( "project.description" ) );

        Properties latin1 = verifier.loadProperties( "latin-1/target/pom.properties" );
        assertEquals( "TEST-CHARS: \u00C4\u00D6\u00DC\u00E4\u00F6\u00FC\u00DF", latin1.getProperty( "project.description" ) );
    }

}
