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
 * This is a test set for <a href="https://issues.apache.org/jira/browse/MNG-2591">MNG-2591</a>.
 * 
 * @author Benjamin Bentmann
 *
 */
public class MavenITmng2591MergeInheritedPluginConfigTest
    extends AbstractMavenIntegrationTestCase
{

    public MavenITmng2591MergeInheritedPluginConfigTest()
    {
        super( "(2.0.7,)" );
    }

    /**
     * Test aggregation of list configuration items for build plugins when using 'combine.children=append' attribute.
     * This variation of the test does not employ profiles.
     */
    public void testitNoProfile()
        throws Exception
    {
        testit( "no-profile" );
    }

    /**
     * Test aggregation of list configuration items for build plugins when using 'combine.children=append' attribute.
     * This variation of the test employs active profiles which however are empty and should not change the result.
     */
    public void testitWithProfile()
        throws Exception
    {
        testit( "with-profile" );
    }

    private void testit( String project )
        throws Exception
    {
        File testDir = ResourceExtractor.simpleExtractResources( getClass(), "/mng-2591/" + project );
        Verifier verifier = newVerifier( testDir.getAbsolutePath() );
        verifier.setAutoclean( false );
        verifier.deleteDirectory( "subproject/target" );
        verifier.executeGoal( "validate" );
        verifier.verifyErrorFreeLog();
        verifier.resetStreams();

        Properties props = verifier.loadProperties( "subproject/target/config.properties" );

        assertEquals( "8", props.getProperty( "stringParams" ) );
        assertEquals( "PARENT-1", props.getProperty( "stringParams.0" ) );
        assertEquals( "PARENT-3", props.getProperty( "stringParams.1" ) );
        assertEquals( "PARENT-2", props.getProperty( "stringParams.2" ) );
        assertEquals( "PARENT-4", props.getProperty( "stringParams.3" ) );
        assertEquals( "CHILD-1", props.getProperty( "stringParams.4" ) );
        assertEquals( "CHILD-3", props.getProperty( "stringParams.5" ) );
        assertEquals( "CHILD-2", props.getProperty( "stringParams.6" ) );
        assertEquals( "CHILD-4", props.getProperty( "stringParams.7" ) );

        assertEquals( "8", props.getProperty( "listParam" ) );
        assertEquals( "PARENT-1", props.getProperty( "listParam.0" ) );
        assertEquals( "PARENT-3", props.getProperty( "listParam.1" ) );
        assertEquals( "PARENT-2", props.getProperty( "listParam.2" ) );
        assertEquals( "PARENT-4", props.getProperty( "listParam.3" ) );
        assertEquals( "CHILD-1", props.getProperty( "listParam.4" ) );
        assertEquals( "CHILD-3", props.getProperty( "listParam.5" ) );
        assertEquals( "CHILD-2", props.getProperty( "listParam.6" ) );
        assertEquals( "CHILD-4", props.getProperty( "listParam.7" ) );
    }

}
