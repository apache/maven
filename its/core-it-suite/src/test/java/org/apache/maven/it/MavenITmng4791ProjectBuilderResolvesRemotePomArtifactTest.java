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
 * This is a test set for <a href="https://issues.apache.org/jira/browse/MNG-4791">MNG-4791</a>.
 *
 * @author Benjamin Bentmann
 */
public class MavenITmng4791ProjectBuilderResolvesRemotePomArtifactTest
    extends AbstractMavenIntegrationTestCase
{

    public MavenITmng4791ProjectBuilderResolvesRemotePomArtifactTest()
    {
        super( "[2.0.3,3.0-alpha-1),[3.0-beta-4,)" );
    }

    /**
     * Test that the project builder resolves the input artifact when building remote POMs if the input artifact
     * happens to be of type "pom".
     *
     * @throws Exception in case of failure
     */
    @Test
    public void testit()
        throws Exception
    {
        File testDir = ResourceExtractor.simpleExtractResources( getClass(), "/mng-4791" );

        Verifier verifier = newVerifier( testDir.getAbsolutePath() );
        verifier.setAutoclean( false );
        verifier.deleteDirectory( "target" );
        verifier.deleteArtifacts( "org.apache.maven.its.mng4791" );
        verifier.addCliOption( "-s" );
        verifier.addCliOption( "settings.xml" );
        verifier.filterFile( "settings-template.xml", "settings.xml", "UTF-8", verifier.newDefaultFilterProperties() );
        verifier.addCliArgument( "validate" );
        verifier.execute();
        verifier.verifyErrorFreeLog();

        Properties props = verifier.loadProperties( "target/pom.properties" );

        assertEquals( "0.1-20100902.190819-1",
            props.getProperty( "org.apache.maven.its.mng4791:a:pom:0.1-SNAPSHOT.version" ) );
        String path = props.getProperty( "org.apache.maven.its.mng4791:a:pom:0.1-SNAPSHOT.file" );
        assertTrue( path, path.endsWith( ".pom" ) );
    }

}
