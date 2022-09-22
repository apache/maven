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
import java.util.List;

/**
 * This is a test set for <a href="https://issues.apache.org/jira/browse/MNG-3380">MNG-3380</a>.
 *
 * expected project.getArtifacts() results:
 *
 * org.apache.maven.its.mng3380:direct:jar:1:compile
 * org.apache.maven.its.mng3380.new:transitive:jar:2:compile
 * org.apache.maven.its.mng3380.other:a:jar:1:compile
 * org.apache.maven.its.mng3380.other:b:jar:1:compile
 *
 *
 */
public class MavenITmng3380ManagedRelocatedTransdepsTest
    extends AbstractMavenIntegrationTestCase
{

    public MavenITmng3380ManagedRelocatedTransdepsTest()
    {
        super( "(2.0.9,)" );
    }

    /**
     * Verify that dependency resolution considers dependency management also for relocated artifacts.
     *
     * @throws Exception in case of failure
     */
    public void testitMNG3380()
        throws Exception
    {
        File testDir = ResourceExtractor.simpleExtractResources( getClass(), "/mng-3380" );

        Verifier verifier = newVerifier( testDir.getAbsolutePath() );
        verifier.setAutoclean( false );
        verifier.deleteDirectory( "target" );
        verifier.deleteArtifacts( "org.apache.maven.its.mng3380" );
        verifier.filterFile( "settings-template.xml", "settings.xml", "UTF-8", verifier.newDefaultFilterProperties() );
        verifier.addCliOption( "--settings" );
        verifier.addCliOption( "settings.xml" );
        verifier.executeGoal( "validate" );
        verifier.verifyErrorFreeLog();
        verifier.resetStreams();

        List<String> artifacts = verifier.loadLines( "target/artifacts.txt", "UTF-8" );
        assertEquals( 4, artifacts.size() );
        assertEquals( "org.apache.maven.its.mng3380:direct:jar:1", artifacts.get( 0 ) );
        assertEquals( "org.apache.maven.its.mng3380.new:transitive:jar:2", artifacts.get( 1 ) );
        assertEquals( "org.apache.maven.its.mng3380.other:a:jar:1", artifacts.get( 2 ) );
        assertEquals( "org.apache.maven.its.mng3380.other:b:jar:1", artifacts.get( 3 ) );

        List<String> paths = verifier.loadLines( "target/test.txt", "UTF-8" );
        assertEquals( 6, paths.size() );
        assertEquals( "direct-1.jar", paths.get( 2 ) );
        assertEquals( "transitive-2.jar", paths.get( 3 ) );
        assertEquals( "a-1.jar", paths.get( 4 ) );
        assertEquals( "b-1.jar", paths.get( 5 ) );
    }

}
