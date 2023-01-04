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
 * This is a test set for <a href="https://issues.apache.org/jira/browse/MNG-3099">MNG-3099</a>.
 *
 * @author <a href="mailto:brianf@apache.org">Brian Fox</a>
 */
public class MavenITmng3099SettingsProfilesWithNoPomTest
    extends AbstractMavenIntegrationTestCase
{

    public MavenITmng3099SettingsProfilesWithNoPomTest()
    {
        super( "(2.0.8,)" ); // 2.0.9+
    }

    /**
     * Verify that (active) profiles from the settings are effective even if no POM is in use (e.g. archetype:create).
     * In more detail, this means the plugin can be resolved from the repositories given in the settings and the plugin
     * can access properties defined by the profiles.
     *
     * @throws Exception in case of failure
     */
    @Test
    public void testit()
        throws Exception
    {
        File testDir = ResourceExtractor.simpleExtractResources( getClass(), "/mng-3099" );

        Verifier verifier = newVerifier( testDir.getAbsolutePath() );
        verifier.setAutoclean( false );
        verifier.deleteDirectory( "target" );
        verifier.deleteArtifacts( "org.apache.maven.its.mng3099" );
        verifier.filterFile( "settings-template.xml", "settings.xml", "UTF-8", verifier.newDefaultFilterProperties() );
        verifier.addCliOption( "-s" );
        verifier.addCliOption( "settings.xml" );
        verifier.executeGoal( "org.apache.maven.its.mng3099:maven-mng3099-plugin:0.1:touch" );
        verifier.verifyErrorFreeLog();

        verifier.verifyFilePresent( "target/PASSED.txt" );
        verifier.verifyFileNotPresent( "target/touch.txt" );
    }

}
