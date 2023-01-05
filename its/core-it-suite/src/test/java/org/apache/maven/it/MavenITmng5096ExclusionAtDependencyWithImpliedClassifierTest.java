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

import org.junit.jupiter.api.Test;

/**
 * This is a test set for <a href="https://issues.apache.org/jira/browse/MNG-5096">MNG-5096</a>.
 *
 * @author Benjamin Bentmann
 */
public class MavenITmng5096ExclusionAtDependencyWithImpliedClassifierTest
    extends AbstractMavenIntegrationTestCase
{

    public MavenITmng5096ExclusionAtDependencyWithImpliedClassifierTest()
    {
        super( "[2.0.9,3.0-alpha-1),[3.0.4,)" );
    }

    /**
     * Verify that exclusions on dependencies whose type implies a classifier are effective. For those dependencies,
     * the versionless management key of the dependency is different from the versionless id of the resulting artifact
     * which needs to be considered.
     *
     * @throws Exception in case of failure
     */
    @Test
    public void testit()
        throws Exception
    {
        File testDir = ResourceExtractor.simpleExtractResources( getClass(), "/mng-5096" );

        Verifier verifier = newVerifier( testDir.getAbsolutePath() );
        verifier.setAutoclean( false );
        verifier.deleteDirectory( "target" );
        verifier.deleteArtifacts( "org.apache.maven.its.mng5096" );
        verifier.addCliArgument( "-s" );
        verifier.addCliArgument( "settings.xml" );
        verifier.filterFile( "settings-template.xml", "settings.xml", "UTF-8" );
        verifier.addCliArgument( "validate" );
        verifier.execute();
        verifier.verifyErrorFreeLog();

        List<String> compile = verifier.loadLines( "target/compile.txt", "UTF-8" );

        assertFalse( compile.toString(), compile.contains( "a-0.1.jar" ) );
        assertTrue( compile.toString(), compile.contains( "b-0.1.jar" ) );
    }

}
