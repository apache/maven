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

import org.apache.maven.it.util.ResourceExtractor;

/**
 * This is a test set for <a href="https://issues.apache.org/jira/browse/MNG-6210">MNG-6210</a>:
 * check that Maven loads core extensions and {@code @SessionScoped} and
 * {@code @MojoExecutionScoped} components contributed by <code>.mvn/extensions.xml</code> are
 * available to regular plugins.
 */
public class MavenITmng6210CoreExtensionsCustomScopesTest
    extends AbstractMavenIntegrationTestCase
{
    public MavenITmng6210CoreExtensionsCustomScopesTest()
    {
        super( "(3.5.0,)" );
    }

    public void testCoreExtensionCustomScopes()
        throws Exception
    {
        File testDir = ResourceExtractor.simpleExtractResources( getClass(), "/mng-6210-core-extensions-scopes" );

        Verifier verifier = newVerifier( testDir.getAbsolutePath() );
        verifier.filterFile( "settings-template.xml", "settings.xml", "UTF-8", verifier.newDefaultFilterProperties() );

        verifier = newVerifier( new File( testDir, "client" ).getAbsolutePath() );
        verifier.deleteDirectory( "target" );
        verifier.deleteArtifacts( "org.apache.maven.its.it-core-extensions-custom-scopes" );
        verifier.getCliOptions().add( "-s" );
        verifier.getCliOptions().add( new File( testDir, "settings.xml" ).getAbsolutePath() );
        verifier.executeGoal( "validate" );
        verifier.verifyErrorFreeLog();
        verifier.resetStreams();
    }
}
