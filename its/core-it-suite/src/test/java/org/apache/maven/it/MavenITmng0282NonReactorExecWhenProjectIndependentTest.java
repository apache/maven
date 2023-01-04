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
 * This is a test set for <a href="https://issues.apache.org/jira/browse/MNG-282">MNG-282</a>.
 *
 * @author John Casey
 *
 */
public class MavenITmng0282NonReactorExecWhenProjectIndependentTest
    extends AbstractMavenIntegrationTestCase
{
    public MavenITmng0282NonReactorExecWhenProjectIndependentTest()
    {
        super( ALL_MAVEN_VERSIONS );
    }

    /**
     * Test non-reactor behavior when plugin declares "@requiresProject false"
     *
     * @throws Exception in case of failure
     */
    @Test
    public void testitMNG282()
        throws Exception
    {
        File testDir = ResourceExtractor.simpleExtractResources( getClass(), "/mng-0282" );

        Verifier verifier = newVerifier( testDir.getAbsolutePath() );
        verifier.setAutoclean( false );
        verifier.deleteDirectory( "target" );
        verifier.deleteDirectory( "subproject/target" );
        verifier.addCliArgument( "org.apache.maven.its.plugins:maven-it-plugin-no-project:light-touch" );
        verifier.execute();
        verifier.verifyFilePresent( "target/touch.txt" );
        verifier.verifyFileNotPresent( "subproject/target/touch.txt" );
        verifier.verifyErrorFreeLog();
    }

}
