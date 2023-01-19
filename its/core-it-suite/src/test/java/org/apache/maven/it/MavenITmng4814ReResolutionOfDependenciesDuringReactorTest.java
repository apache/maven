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
import java.util.ArrayList;
import java.util.List;

import org.junit.jupiter.api.Test;

/**
 * This is a test set for <a href="https://issues.apache.org/jira/browse/MNG-4814">MNG-4814</a>.
 *
 * @author Benjamin Bentmann
 */
public class MavenITmng4814ReResolutionOfDependenciesDuringReactorTest
    extends AbstractMavenIntegrationTestCase
{

    public MavenITmng4814ReResolutionOfDependenciesDuringReactorTest()
    {
        super( "[3.0,)" );
    }

    /**
     * Verify that dependency resolution by an aggregator before the build has actually produced any artifacts
     * doesn't prevent later resolution of project artifacts from the reactor if the aggregator originally resolved
     * them from the remote repo.
     *
     * @throws Exception in case of failure
     */
    @Test
    public void testit()
        throws Exception
    {
        File testDir = ResourceExtractor.simpleExtractResources( getClass(), "/mng-4814" );

        Verifier verifier = newVerifier( testDir.getAbsolutePath() );
        verifier.setAutoclean( false );
        verifier.deleteDirectory( "target" );
        verifier.deleteDirectory( "consumer/target" );
        verifier.deleteArtifacts( "org.apache.maven.its.mng4814" );
        verifier.addCliArgument( "-s" );
        verifier.addCliArgument( "settings.xml" );
        verifier.filterFile( "settings-template.xml", "settings.xml", "UTF-8" );
        verifier.addCliArguments( "validate",
            "org.apache.maven.its.plugins:maven-it-plugin-dependency-resolution:2.1-SNAPSHOT:aggregate-test" );
        verifier.execute();
        verifier.verifyErrorFreeLog();

        List<String> compile = verifier.loadLines( "consumer/target/compile.txt", "UTF-8" );

        assertFalse( compile.toString(), compile.contains( "0.1-SNAPSHOT/producer-0.1-SNAPSHOT.jar" ) );
        assertTrue( compile.toString(), compile.contains( "producer/pom.xml" ) );
    }

}
