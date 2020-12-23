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

import org.apache.maven.it.util.ResourceExtractor;

import java.io.File;

/**
 * This is a test set for <a href="https://issues.apache.org/jira/browse/MNG-4148">MNG-4148</a>.
 *
 * @author John Casey
 */
public class MavenITmng4148DepPomInterpWithSettingsProfilesTest
    extends AbstractMavenIntegrationTestCase
{

    public MavenITmng4148DepPomInterpWithSettingsProfilesTest()
    {
        // TODO: Disabled for 3.x due to its controversial effects, see also http://www.mail-archive.com/dev@maven.apache.org/msg82166.html
        super( "(2.2.1,3.0-alpha-1)" );
    }

    /**
     * Test that a property from the settings profile that used in the
     * version for a dependency is interpolated when the spec is a transitive dependency
     * (declared in the POM of a direct dependency of the current project).
     */
    public void testit()
        throws Exception
    {
        File testDir = ResourceExtractor.simpleExtractResources( getClass(), "/mng-4148" );

        Verifier verifier = newVerifier( testDir.getAbsolutePath() );

        verifier.deleteDirectory( "target" );
        verifier.deleteArtifacts( "org.apache.maven.its.mng4148" );

        verifier.setAutoclean( false );

        verifier.addCliOption( "-s" );
        verifier.addCliOption( "settings.xml" );

        verifier.filterFile( "settings-template.xml", "settings.xml", "UTF-8", verifier.newDefaultFilterProperties() );

        verifier.executeGoal( "validate" );

        verifier.verifyErrorFreeLog();
        verifier.resetStreams();
    }

}
