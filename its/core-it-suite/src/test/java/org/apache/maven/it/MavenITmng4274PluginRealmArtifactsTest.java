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
 * This is a test set for <a href="https://issues.apache.org/jira/browse/MNG-4274">MNG-4274</a>.
 *
 * @author Benjamin Bentmann
 */
public class MavenITmng4274PluginRealmArtifactsTest
    extends AbstractMavenIntegrationTestCase
{

    public MavenITmng4274PluginRealmArtifactsTest()
    {
        super( "[3.0-alpha-3,)" );
    }

    /**
     * Verify that plugins with an undeclared dependency on plexus-utils that is brought in as a transitive dependency
     * of some Maven core artifact get the proper version of plexus-utils. For clarity, the proper version is the
     * version that the original core artifact specified as dependency, not the version shipped with the current core.
     *
     * @throws Exception in case of failure
     */
    @Test
    public void testit()
        throws Exception
    {
        File testDir = ResourceExtractor.simpleExtractResources( getClass(), "/mng-4274" );

        Verifier verifier = newVerifier( testDir.getAbsolutePath() );
        verifier.setAutoclean( false );
        verifier.deleteDirectory( "target" );
        verifier.deleteArtifact( "org.apache.maven", "maven-core", "2.0.4274", "jar" );
        verifier.deleteArtifact( "org.apache.maven", "maven-core", "2.0.4274", "pom" );
        verifier.deleteArtifact( "org.codehaus.plexus", "plexus-utils", "1.1.4274", "jar" );
        verifier.deleteArtifact( "org.codehaus.plexus", "plexus-utils", "1.1.4274", "pom" );
        verifier.filterFile( "settings-template.xml", "settings.xml", "UTF-8", verifier.newDefaultFilterProperties() );
        verifier.addCliOption( "-s" );
        verifier.addCliOption( "settings.xml" );
        verifier.executeGoal( "validate" );
        verifier.verifyErrorFreeLog();
        verifier.resetStreams();

        Properties props = verifier.loadProperties( "target/class.properties" );
        assertNotNull( props.getProperty( "org.apache.maven.its.mng4274.CoreIt" ) );
    }

}
