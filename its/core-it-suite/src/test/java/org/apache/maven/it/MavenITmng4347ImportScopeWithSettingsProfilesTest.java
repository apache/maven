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
 * This is a test set for <a href="https://issues.apache.org/jira/browse/MNG-4347">MNG-4347</a>.
 *
 * @author John Casey
 */
public class MavenITmng4347ImportScopeWithSettingsProfilesTest
    extends AbstractMavenIntegrationTestCase
{

    public MavenITmng4347ImportScopeWithSettingsProfilesTest()
    {
        super( "(2.2.1,]" );
    }

    /**
     * Test that profiles from settings.xml will be used to resolve import-scoped dependency POMs.
     * In this case, the settings profile enables snapshot resolution on the central repository, which
     * is required to resolve the import-scoped POM with a SNAPSHOT version.
     *
     * @throws Exception in case of failure
     */
    @Test
    public void testit()
        throws Exception
    {
        File testDir = ResourceExtractor.simpleExtractResources( getClass(), "/mng-4347" );

        Verifier verifier = newVerifier( testDir.getAbsolutePath() );

        verifier.deleteDirectory( "target" );
        verifier.deleteArtifacts( "org.apache.maven.its.mng4347" );

        verifier.setAutoclean( false );

        verifier.addCliOption( "-s" );
        verifier.addCliOption( "settings.xml" );

        verifier.filterFile( "settings-template.xml", "settings.xml", "UTF-8", verifier.newDefaultFilterProperties() );

        verifier.addCliArgument( "validate" );
        verifier.execute();

        verifier.verifyErrorFreeLog();
    }

}
