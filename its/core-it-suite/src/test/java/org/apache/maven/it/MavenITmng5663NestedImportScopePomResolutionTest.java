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
 * This is a test set for <a href="https://issues.apache.org/jira/browse/MNG-5663">MNG-5663</a>:
 *
 * The test POM references an import scope POM, which also has a dependency on an import scope POM.
 *
 * The 2nd import scope POM is found in a repository that is only defined in the test POM. The test confirms
 * that dependencies are successfully resolved for this setup.
 *
 */
public class MavenITmng5663NestedImportScopePomResolutionTest
    extends AbstractMavenIntegrationTestCase
{

    public MavenITmng5663NestedImportScopePomResolutionTest()
    {
        super( "[3.0.4,3.2.2),(3.2.2,)" );
    }

    @Test
    public void testitMNG5639()
        throws Exception
    {
        File testDir = ResourceExtractor.simpleExtractResources( getClass(), "/mng-5663-nested-import-scope-pom-resolution");

        Verifier verifier = newVerifier( testDir.getAbsolutePath() );
        verifier.deleteArtifacts( "org.apache.maven.its.mng5663" );

        verifier.filterFile( "pom-template.xml", "pom.xml", "UTF-8", verifier.newDefaultFilterProperties() );
        verifier.filterFile( "settings-template.xml", "settings.xml", "UTF-8", verifier.newDefaultFilterProperties() );
        verifier.addCliOption( "--settings" );
        verifier.addCliOption( "settings.xml" );

        verifier.addCliArgument( "install" );
        verifier.execute();
        verifier.verifyErrorFreeLog();

        verifier.verifyArtifactPresent( "org.apache.maven.its.mng5663", "c", "0.1", "jar" );

    }



}
