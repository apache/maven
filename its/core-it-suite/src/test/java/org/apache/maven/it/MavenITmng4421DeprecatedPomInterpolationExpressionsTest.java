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
import java.util.Iterator;
import java.util.List;
import java.util.Properties;

/**
 * This is a test set for <a href="http://jira.codehaus.org/browse/MNG-4421">MNG-4421</a>.
 * 
 * @author Benjamin Bentmann
 */
public class MavenITmng4421DeprecatedPomInterpolationExpressionsTest
    extends AbstractMavenIntegrationTestCase
{

    public MavenITmng4421DeprecatedPomInterpolationExpressionsTest()
    {
        super( "[3.0-alpha-3,)" );
    }

    /**
     * Test that expressions of the form ${pom.*} and {*} referring to the model cause build warnings.
     */
    public void testit()
        throws Exception
    {
        File testDir = ResourceExtractor.simpleExtractResources( getClass(), "/mng-4421" );

        Verifier verifier = new Verifier( testDir.getAbsolutePath() );
        verifier.setAutoclean( false );
        verifier.deleteDirectory( "target" );
        verifier.executeGoal( "validate" );
        verifier.verifyErrorFreeLog();
        verifier.resetStreams();

        Properties props = verifier.loadProperties( "target/pom.properties" );
        assertEquals( "0.1", props.getProperty( "project.properties.property1" ) );
        assertEquals( "0.1", props.getProperty( "project.properties.property2" ) );

        List lines = verifier.loadLines( "log.txt", null );
        
        boolean warnedPomPrefix = false;
        boolean warnedEmptyPrefix = false;
        
        for ( Iterator it = lines.iterator(); it.hasNext(); )
        {
            String line = (String) it.next();
            if ( line.startsWith( "[WARN" ) )
            {
                if ( line.indexOf( "${pom.version}" ) >= 0 )
                {
                    warnedPomPrefix = true;
                }
                if ( line.indexOf( "${version}" ) >= 0 )
                {
                    warnedEmptyPrefix = true;
                }
            }
        }

        assertTrue( warnedPomPrefix );
        assertTrue( warnedEmptyPrefix );
    }

}
