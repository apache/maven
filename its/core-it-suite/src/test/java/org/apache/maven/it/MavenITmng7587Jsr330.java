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

import org.apache.maven.shared.verifier.Verifier;
import org.apache.maven.shared.verifier.util.ResourceExtractor;
import org.codehaus.plexus.util.Os;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledOnJre;
import org.junit.jupiter.api.condition.JRE;

import java.io.File;

/**
 * This is a test set for <a href="https://issues.apache.org/jira/browse/MNG-7587">MNG-7587</a>.
 * Simply verifies that plexus component using JSR330 and compiled with JDK 17 bytecode can
 * work on maven.
 */
class MavenITmng7587Jsr330
    extends AbstractMavenIntegrationTestCase
{

    public MavenITmng7587Jsr330()
    {
        // affected Maven versions: 3.9.2 and 4.0.0-alpha-5
        super( "(3.9.2,3.999),(4.0.0-alpha-5,)" );
    }

    /**
     * Verify components can be written using JSR330 on JDK 17.
     *
     * @throws Exception in case of failure
     */
    @Test
    @EnabledOnJre(JRE.JAVA_17)
    void testJdk17() throws Exception
    {
        File testDir = ResourceExtractor.simpleExtractResources( getClass(), "/mng-7587-jsr330").getAbsoluteFile();

        final Verifier pluginVerifier = newVerifier( new File( testDir, "plugin" ).getPath() );
        pluginVerifier.addCliArgument( "clean" );
        pluginVerifier.addCliArgument( "install" );
        pluginVerifier.addCliArgument( "-V" );
        pluginVerifier.execute();
        pluginVerifier.verifyErrorFreeLog();

        final Verifier consumerVerifier = newVerifier( new File( testDir, "consumer" ).getPath() );
        consumerVerifier.addCliArgument( "clean" );
        consumerVerifier.addCliArgument( "verify" );
        consumerVerifier.addCliArgument( "-V" );
        consumerVerifier.execute();
        consumerVerifier.verifyErrorFreeLog();
    }

}
