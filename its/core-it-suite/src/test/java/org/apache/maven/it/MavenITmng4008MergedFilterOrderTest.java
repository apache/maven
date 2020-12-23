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
import java.util.Properties;

/**
 * This is a test set for <a href="https://issues.apache.org/jira/browse/MNG-4008">MNG-4008</a>.
 *
 * @author Benjamin Bentmann
 *
 */
public class MavenITmng4008MergedFilterOrderTest
    extends AbstractMavenIntegrationTestCase
{

    public MavenITmng4008MergedFilterOrderTest()
    {
        super( ALL_MAVEN_VERSIONS );
    }

    /**
     * Verify that filter definitions are properly merged.
     */
    public void testitMNG4008()
        throws Exception
    {
        File testDir = ResourceExtractor.simpleExtractResources( getClass(), "/mng-4008" );

        Verifier verifier = newVerifier( new File( testDir, "sub" ).getAbsolutePath() );
        verifier.setAutoclean( false );
        verifier.deleteDirectory( "target" );
        verifier.executeGoal( "validate" );
        verifier.verifyErrorFreeLog();
        verifier.resetStreams();

        Properties modelProps = verifier.loadProperties( "target/model.properties" );

        assertEquals( "7", modelProps.getProperty( "project.build.filters" ) );

        assertTrue( modelProps.getProperty( "project.build.filters.0" ).endsWith( "child-a.properties" ) );
        assertTrue( modelProps.getProperty( "project.build.filters.1" ).endsWith( "child-c.properties" ) );
        assertTrue( modelProps.getProperty( "project.build.filters.2" ).endsWith( "child-b.properties" ) );
        assertTrue( modelProps.getProperty( "project.build.filters.3" ).endsWith( "child-d.properties" ) );
        assertTrue( modelProps.getProperty( "project.build.filters.4" ).endsWith( "parent-c.properties" ) );
        assertTrue( modelProps.getProperty( "project.build.filters.5" ).endsWith( "parent-b.properties" ) );
        assertTrue( modelProps.getProperty( "project.build.filters.6" ).endsWith( "parent-d.properties" ) );
    }

}
