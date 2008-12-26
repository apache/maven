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
import org.apache.maven.it.util.Os;

import java.io.File;
import java.util.Properties;

/**
 * This is a test set for <a href="http://jira.codehaus.org/browse/MNG-3944">MNG-3944</a>.
 * 
 * @author Benjamin Bentmann
 * @version $Id$
 */
public class MavenITmng3944BasedirInterpolationTest
    extends AbstractMavenIntegrationTestCase
{

    public MavenITmng3944BasedirInterpolationTest()
    {
    }

    /**
     * Test that interpolation of ${basedir} works for a POM that is not named "pom.xml"
     */
    public void testitMNG3944()
        throws Exception
    {
        File testDir = ResourceExtractor.simpleExtractResources( getClass(), "/mng-3944" );

        Verifier verifier = new Verifier( testDir.getAbsolutePath() );
        verifier.setAutoclean( false );
        verifier.deleteDirectory( "target" );
        verifier.getCliOptions().add( "-f" );
        verifier.getCliOptions().add( new File( testDir, "pom-with-unusual-name.xml" ).getAbsolutePath() );
        verifier.executeGoal( "validate" );
        verifier.verifyErrorFreeLog();
        verifier.resetStreams();

        verifier.assertFilePresent( "target/basedir.properties" );
        Properties props = verifier.loadProperties( "target/basedir.properties" );
        assertEquals( testDir, new File( props.getProperty( "project.properties.prop0" ) ) );
        assertEquals( testDir, new File( props.getProperty( "project.properties.prop1" ) ) );
    }

}
