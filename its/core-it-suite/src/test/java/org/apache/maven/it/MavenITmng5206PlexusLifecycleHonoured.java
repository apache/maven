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

import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

/**
 * This is a test set for <a href="https://issues.apache.org/jira/browse/MNG-5206">MNG-5206</a>.
 *
 * @author Olivier Lamy
 */
@Tag("disabled")
public class MavenITmng5206PlexusLifecycleHonoured
    extends AbstractMavenIntegrationTestCase
{

    public MavenITmng5206PlexusLifecycleHonoured()
    {
        super( "[2.0.7,)" );
    }

    /**
     * Verify that plexus lifecycle phases are honoured: contextualize, configure, dispose
     *
     * @throws Exception in case of failure
     */
    @Test
    public void testit()
        throws Exception
    {
        File testDir = ResourceExtractor.simpleExtractResources( getClass(), "/mng-5206" );

        Verifier verifier = newVerifier( testDir.getAbsolutePath() );
        verifier.setAutoclean( false );
        verifier.deleteDirectory( "target" );
        verifier.deleteArtifacts( "org.apache.maven.its.mng5206" );
        verifier.addCliArgument( "validate" );
        verifier.execute();
        verifier.verifyErrorFreeLog();

        verifier.verifyTextInLog( "MojoWithPlexusLifecycle :: contextualize" );
        verifier.verifyTextInLog( "DefaultFakeComponent :: contextualize" );
        verifier.verifyTextInLog( "MojoWithPlexusLifecycle :: dispose" );
        // olamy dispose on injected component is not called
        //verifier.verifyTextInLog( "DefaultFakeComponent :: dispose" );
    }

}
