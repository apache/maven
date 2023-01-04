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
import java.util.Collection;
import java.util.Collections;

import org.junit.jupiter.api.Test;

/**
 * This is a test set for <a href="https://issues.apache.org/jira/browse/MNG-818">MNG-818</a>.
 *
 * @author Brett Porter
 *
 */
public class MavenITmng0818WarDepsNotTransitiveTest
    extends AbstractMavenIntegrationTestCase
{
    public MavenITmng0818WarDepsNotTransitiveTest()
    {
        super( ALL_MAVEN_VERSIONS );
    }

    /**
     * Test that depending on a WAR doesn't also get its dependencies transitively.
     *
     * @throws Exception in case of failure
     */
    @Test
    public void testitMNG0818()
        throws Exception
    {
        File testDir = ResourceExtractor.simpleExtractResources( getClass(), "/mng-0818" );

        Verifier verifier = newVerifier( testDir.getAbsolutePath() );
        verifier.setAutoclean( false );
        verifier.deleteDirectory( "target" );
        verifier.deleteArtifacts( "org.apache.maven.its.it0080" );
        verifier.filterFile( "settings-template.xml", "settings.xml", "UTF-8", verifier.newDefaultFilterProperties() );
        verifier.addCliOption( "--settings" );
        verifier.addCliOption( "settings.xml" );
        verifier.executeGoal( "validate" );
        verifier.verifyErrorFreeLog();

        Collection<String> artifacts = verifier.loadLines( "target/artifacts.txt", "UTF-8" );
        assertEquals( Collections.singletonList( "org.apache.maven.its.it0080:war:war:0.1" ), artifacts );
    }

}
