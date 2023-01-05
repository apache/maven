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
 * This is a test set for <a href="https://issues.apache.org/jira/browse/MNG-3899">MNG-3899</a>.
 *
 * @author Benjamin Bentmann
 *
 */
public class MavenITmng3899ExtensionInheritanceTest
    extends AbstractMavenIntegrationTestCase
{

    public MavenITmng3899ExtensionInheritanceTest()
    {
        super( "(2.0.10,2.1.0-M1),(2.1.0-M1,)" );
    }

    /**
     * Test that build extensions are properly merged during inheritance.
     *
     * @throws Exception in case of failure
     */
    @Test
    public void testitMNG3899()
        throws Exception
    {
        File testDir = ResourceExtractor.simpleExtractResources( getClass(), "/mng-3899" );

        Verifier verifier = newVerifier( new File( testDir, "sub" ).getAbsolutePath() );
        verifier.setAutoclean( false );
        verifier.deleteDirectory( "target" );
        verifier.deleteArtifacts( "org.apache.maven.its.mng3899" );
        verifier.filterFile( "settings-template.xml", "settings.xml", "UTF-8" );
        verifier.addCliArgument( "--settings" );
        verifier.addCliArgument( "settings.xml" );
        verifier.addCliArgument( "validate" );
        verifier.execute();
        verifier.verifyErrorFreeLog();

        Properties props = verifier.loadProperties( "target/extension.properties" );
        assertEquals( "3", props.getProperty( "project.build.extensions" ) );
        assertEquals( "b", props.getProperty( "project.build.extensions.0.artifactId" ) );
        assertEquals( "0.1", props.getProperty( "project.build.extensions.0.version" ) );
        assertEquals( "a", props.getProperty( "project.build.extensions.1.artifactId" ) );
        assertEquals( "0.2", props.getProperty( "project.build.extensions.1.version" ) );
        assertEquals( "c", props.getProperty( "project.build.extensions.2.artifactId" ) );
        assertEquals( "0.1", props.getProperty( "project.build.extensions.2.version" ) );
    }

}
