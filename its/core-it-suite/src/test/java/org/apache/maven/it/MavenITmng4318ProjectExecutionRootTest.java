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

/**
 * This is a test set for <a href="https://issues.apache.org/jira/browse/MNG-4318">MNG-4318</a> and
 * <a href="https://issues.apache.org/jira/browse/MNG-5014">MNG-5014</a>.
 *
 * @author Benjamin Bentmann
 */
public class MavenITmng4318ProjectExecutionRootTest
    extends AbstractMavenIntegrationTestCase
{

    public MavenITmng4318ProjectExecutionRootTest()
    {
        super( "[2.0.4,3.0-alpha-1),[3.0.3,)" );
    }

    /**
     * Verify that MavenProject.isExecutionRoot() is properly set within a reactor.
     *
     * @throws Exception in case of failure
     */
    public void testit()
        throws Exception
    {
        File testDir = ResourceExtractor.simpleExtractResources( getClass(), "/mng-4318" );

        Verifier verifier = newVerifier( testDir.getAbsolutePath() );
        verifier.setAutoclean( false );
        verifier.deleteDirectory( "target" );
        verifier.deleteDirectory( "sub-1/target" );
        verifier.deleteDirectory( "sub-2/target" );
        verifier.deleteDirectory( "sub-2/sub-3/target" );
        verifier.executeGoal( "validate" );
        verifier.verifyErrorFreeLog();
        verifier.resetStreams();

        Properties props;

        props = verifier.loadProperties( "target/project.properties" );
        assertEquals( "true", props.getProperty( "project.executionRoot" ) );

        props = verifier.loadProperties( "sub-1/target/project.properties" );
        assertEquals( "false", props.getProperty( "project.executionRoot" ) );
        assertEquals( "true", props.getProperty( "project.parent.executionRoot" ) );

        props = verifier.loadProperties( "sub-2/target/project.properties" );
        assertEquals( "false", props.getProperty( "project.executionRoot" ) );
        assertEquals( "true", props.getProperty( "project.parent.executionRoot" ) );

        props = verifier.loadProperties( "sub-2/sub-3/target/project.properties" );
        assertEquals( "false", props.getProperty( "project.executionRoot" ) );
        assertEquals( "false", props.getProperty( "project.parent.executionRoot" ) );
        assertEquals( "true", props.getProperty( "project.parent.parent.executionRoot" ) );
    }

}
