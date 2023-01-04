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

import org.junit.jupiter.api.Test;

/**
 * This is a test set for <a href="https://issues.apache.org/jira/browse/MNG-870">MNG-870</a>.
 *
 * @author Benjamin Bentmann
 *
 */
public class MavenITmng0870ReactorAwarePluginDiscoveryTest
    extends AbstractMavenIntegrationTestCase
{

    public MavenITmng0870ReactorAwarePluginDiscoveryTest()
    {
        super( ALL_MAVEN_VERSIONS );
    }

    /**
     * Test that the reactor can resolve plugins that have just been built by a previous module and are not yet
     * installed to the local repo.
     *
     * @throws Exception in case of failure
     */
    @Test
    public void testitMNG0870()
        throws Exception
    {
        File testDir = ResourceExtractor.simpleExtractResources( getClass(), "/mng-0870" );

        Verifier verifier = newVerifier( testDir.getAbsolutePath() );
        verifier.setAutoclean( false );
        verifier.deleteDirectory( "project/target" );
        verifier.deleteArtifacts( "org.apache.maven.its.mng0870" );
        verifier.filterFile( "settings-template.xml", "settings.xml", "UTF-8", verifier.newDefaultFilterProperties() );
        verifier.addCliArgument( "--settings" );
        verifier.addCliArgument( "settings.xml" );
        verifier.addCliArgument( "initialize" );
        verifier.execute();
        verifier.verifyErrorFreeLog();

        verifier.verifyFilePresent( "project/target/touch.txt" );
    }

}
