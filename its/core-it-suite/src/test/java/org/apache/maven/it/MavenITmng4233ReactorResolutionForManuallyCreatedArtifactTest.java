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
 * This is a test set for <a href="https://issues.apache.org/jira/browse/MNG-4233">MNG-4233</a>.
 *
 * @author Benjamin Bentmann
 */
public class MavenITmng4233ReactorResolutionForManuallyCreatedArtifactTest
    extends AbstractMavenIntegrationTestCase
{

    public MavenITmng4233ReactorResolutionForManuallyCreatedArtifactTest()
    {
        super( "[3.0-alpha-3,)" );
    }

    /**
     * Verify that artifact instances created directly by plugins (i.e. via the artifact factory) can be resolved
     * from the reactor. This case is a subtle variation of MNG-2877, namely not using @requiresDependencyResolution
     * or artifact instances created by the Maven core. In short, reactor resolution should work for any artifact,
     * regardless whether created by the core or a plugin.
     *
     * @throws Exception in case of failure
     */
    @Test
    public void testit()
        throws Exception
    {
        File testDir = ResourceExtractor.simpleExtractResources( getClass(), "/mng-4233" );

        Verifier verifier = newVerifier( testDir.getAbsolutePath() );
        verifier.setAutoclean( false );
        verifier.deleteDirectory( "consumer/target" );
        verifier.addCliArgument( "validate" );
        verifier.execute();
        verifier.verifyErrorFreeLog();

        Properties props = verifier.loadProperties( "consumer/target/artifact.properties" );
        assertEquals( new File( testDir.getCanonicalFile(), "producer/pom.xml" ), new File(
            props.getProperty( "org.apache.maven.its.mng4233:producer:jar:1.0-SNAPSHOT" ) ).getCanonicalFile() );
    }

}
