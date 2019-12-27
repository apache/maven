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
import java.util.Properties;

import org.apache.maven.it.Verifier;
import org.apache.maven.it.util.ResourceExtractor;

/**
 * This is a test set for <a href="https://issues.apache.org/jira/browse/MNG-3810">MNG-3810</a>.
 * 
 * @author Brett Porter
 *
 */
public class MavenITmng3810BadProfileActivationTest
    extends AbstractMavenIntegrationTestCase
{

    public MavenITmng3810BadProfileActivationTest()
    {
        super( "(2.0.10,2.1.0-M1),(2.1.0-M1,3.0-alpha-1),[3.0-alpha-3,)" ); // 2.0.11+, 2.1.0-M2+
    }

    public void testitMNG3810Property()
        throws Exception
    {
        File testDir = ResourceExtractor.simpleExtractResources( getClass(), "/mng-3810/property" );

        Verifier verifier = newVerifier( testDir.getAbsolutePath() );
        verifier.setAutoclean( false );
        verifier.deleteDirectory( "target" );
        try
        {
            verifier.executeGoal( "validate" );
            fail( "Build should not succeed" );
        }
        catch ( Exception e )
        {
            verifier.verifyTextInLog( "The property name is required to activate the profile" );
        }
        verifier.resetStreams();

        Properties props = verifier.loadProperties( "target/profile.properties" );
        assertNull( props.getProperty( "project.properties.pomProperty" ) );
    }

}
