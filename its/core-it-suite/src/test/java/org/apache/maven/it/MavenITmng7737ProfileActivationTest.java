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

import java.io.File;
import java.util.List;

import org.apache.maven.shared.verifier.Verifier;
import org.apache.maven.shared.verifier.util.ResourceExtractor;
import org.codehaus.plexus.util.Os;
import org.junit.jupiter.api.Test;

/**
 * This is a test set for <a href="https://issues.apache.org/jira/browse/MNG-7737">MNG-7737</a>.
 * Simply verifies that various (expected) profiles are properly activated or not.
 *
 */
class MavenITmng7737ProfileActivationTest
    extends AbstractMavenIntegrationTestCase
{

    public MavenITmng7737ProfileActivationTest()
    {
        // affected Maven versions: 3.9.0
        super( "(,3.9.0),(3.9.0,)" );
    }

    /**
     * Verify that profiles are active as expected.
     *
     * @throws Exception in case of failure
     */
    @Test
    void testSingleMojoNoPom()
        throws Exception
    {
        File testDir = ResourceExtractor.simpleExtractResources( getClass(), "/mng-7737-profiles" );

        Verifier verifier = newVerifier( testDir.getAbsolutePath() );
        verifier.addCliArgument( "-s" );
        verifier.addCliArgument( "settings.xml" );
        verifier.addCliArgument( "org.apache.maven.plugins:maven-help-plugin:3.3.0:active-profiles" );
        verifier.addCliArgument( "-Dsettings-property" );
        verifier.addCliArgument( "-Dpom-property" );
        verifier.execute();
        verifier.verifyErrorFreeLog();

        // Example output on my Linux Box w/ Java 17
        //The following profiles are active:
        //
        //- settings-active-default (source: external)
        //- settings-active (source: external)
        //- settings-jdk-8 (source: external)
        //- settings-jdk-11 (source: external)
        //- settings-jdk-17 (source: external)
        //- settings-os-unix (source: external)
        //- settings-property (source: external)
        //- settings-file-exists-present (source: external)
        //- settings-file-missing-absent (source: external)
        //- it-defaults (source: external)
        //- pom-jdk-8 (source: org.apache.maven.its.mng7737:test:0.1)
        //- pom-jdk-11 (source: org.apache.maven.its.mng7737:test:0.1)
        //- pom-jdk-17 (source: org.apache.maven.its.mng7737:test:0.1)
        //- pom-os-unix (source: org.apache.maven.its.mng7737:test:0.1)
        //- pom-property (source: org.apache.maven.its.mng7737:test:0.1)
        //- pom-file-exists-present (source: org.apache.maven.its.mng7737:test:0.1)
        //- pom-file-missing-absent (source: org.apache.maven.its.mng7737:test:0.1)

        verifier.verifyTextInLog( "settings-active-default" );
        verifier.verifyTextInLog( "settings-active" );
        verifier.verifyTextInLog( "settings-jdk"); // enough, as we build on 8+ at least one is present
        verifier.verifyTextInLog( "settings-os-" + Os.OS_FAMILY );
        verifier.verifyTextInLog( "settings-property" );
        verifier.verifyTextInLog( "settings-file-exists-present" );
        verifier.verifyTextInLog( "settings-file-missing-absent" );

        // In case of POM, the pom-active-default documented constraint stands:
        // "This profile will automatically be active for all builds unless
        // **another profile in the same POM** is activated using one of the previously described methods."
        verifier.verifyTextInLog( "pom-jdk"); // enough, as we build on 8+ at least one is present
        verifier.verifyTextInLog( "pom-os-" + Os.OS_FAMILY );
        verifier.verifyTextInLog( "pom-property" );
        verifier.verifyTextInLog( "pom-file-exists-present" );
        verifier.verifyTextInLog( "pom-file-missing-absent" );
    }
}
