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
 * This is a test set for <a href="https://issues.apache.org/jira/browse/MNG-5572">MNG-5572</a>
 * as a response to <a href="https://issues.apache.org/jira/browse/MNG-1911">MNG-1911</a>
 *
 *
 * @author rfscholte
 */
public class MavenITmng5572ReactorPluginExtensionsTest
    extends AbstractMavenIntegrationTestCase
{

    public MavenITmng5572ReactorPluginExtensionsTest()
    {
        super( "[3.2,)" );
    }

    /**
     * Test that Maven warns when one reactor project contains a plugin, and another tries to use it with extensions
     *
     * @throws Exception in case of failure
     */
    @Test
    public void testit()
        throws Exception
    {
        File testDir = ResourceExtractor.simpleExtractResources( getClass(), "/mng-5572-reactor-plugin-extensions" );

        // plugin must be available in local repo, otherwise the project couldn't be built
        Verifier setup = newVerifier( testDir.getAbsolutePath() );
        setup.setAutoclean( true );
        setup.addCliArgument( "-f" );
        setup.addCliArgument( "plugin/pom.xml" );
        setup.addCliArgument( "install" );
        setup.execute();
        setup.verifyErrorFreeLog();

        Verifier verifier = newVerifier( testDir.getAbsolutePath() );
        verifier.setLogFileName( "log2.txt" );
        verifier.setAutoclean( false );
        verifier.addCliArgument( "validate" );
        verifier.execute();
        verifier.verifyErrorFreeLog();
        if ( getMavenVersion().getMajorVersion() <= 3 )
        {
            verifier.verifyTextInLog( "[WARNING] project uses org.apache.maven.its.mng5572:plugin as extensions, which is not possible within the same reactor build. This plugin was pulled from the local repository!" );
        }
        else
        {
            verifier.verifyTextInLog( "[WARNING] 'project' uses 'org.apache.maven.its.mng5572:plugin' as extension which is not possible within the same reactor build. This plugin was pulled from the local repository!" );
        }
    }

}
