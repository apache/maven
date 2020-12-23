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
 * This is a test set for <a href="https://issues.apache.org/jira/browse/MNG-1323">MNG-1323</a>.
 *
 * @author Benjamin Bentmann
 *
 */
public class MavenITmng1323AntrunDependenciesTest
    extends AbstractMavenIntegrationTestCase
{

    public MavenITmng1323AntrunDependenciesTest()
    {
        super( "[3.0-alpha-1,)" );
    }

    /**
     * Verify that project-level plugin dependencies actually apply to the current project only and not the entire
     * reactor.
     */
    public void testitMNG1323()
        throws Exception
    {
        File testDir = ResourceExtractor.simpleExtractResources( getClass(), "/mng-1323" );

        Verifier verifier = newVerifier( testDir.getAbsolutePath() );
        verifier.setAutoclean( false );
        verifier.deleteDirectory( "target" );
        verifier.deleteDirectory( "a/target" );
        verifier.deleteDirectory( "b/target" );
        verifier.deleteDirectory( "c/target" );
        verifier.deleteArtifacts( "org.apache.maven.its.mng1323" );
        verifier.filterFile( "settings-template.xml", "settings.xml", "UTF-8", verifier.newDefaultFilterProperties() );
        verifier.addCliOption( "--settings" );
        verifier.addCliOption( "settings.xml" );
        verifier.executeGoal( "validate" );
        verifier.verifyErrorFreeLog();
        verifier.resetStreams();

        Properties pclProps;

        pclProps = verifier.loadProperties( "target/pcl.properties" );
        assertNull( pclProps.getProperty( "org.apache.maven.its.mng1323.ClassA" ) );
        assertNull( pclProps.getProperty( "org.apache.maven.its.mng1323.ClassB" ) );

        pclProps = verifier.loadProperties( "a/target/pcl.properties" );
        assertNotNull( pclProps.getProperty( "org.apache.maven.its.mng1323.ClassA" ) );
        assertNull( pclProps.getProperty( "org.apache.maven.its.mng1323.ClassB" ) );

        pclProps = verifier.loadProperties( "b/target/pcl.properties" );
        assertNull( pclProps.getProperty( "org.apache.maven.its.mng1323.ClassA" ) );
        assertNotNull( pclProps.getProperty( "org.apache.maven.its.mng1323.ClassB" ) );

        pclProps = verifier.loadProperties( "c/target/pcl.properties" );
        assertNull( pclProps.getProperty( "org.apache.maven.its.mng1323.ClassA" ) );
        assertNull( pclProps.getProperty( "org.apache.maven.its.mng1323.ClassB" ) );
    }

}
