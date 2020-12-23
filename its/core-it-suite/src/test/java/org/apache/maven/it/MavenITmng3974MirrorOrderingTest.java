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

import org.apache.maven.it.Verifier;
import org.apache.maven.it.util.ResourceExtractor;

import java.io.File;

/**
 * This is a test set for <a href="https://issues.apache.org/jira/browse/MNG-3974">MNG-3974</a>.
 *
 * @author Benjamin Bentmann
 *
 */
public class MavenITmng3974MirrorOrderingTest
    extends AbstractMavenIntegrationTestCase
{

    public MavenITmng3974MirrorOrderingTest()
    {
        super( "(2.0.9,2.1.0-M1),(2.1.0-M1,3.0-alpha-1),(3.0-alpha-1,)" );
    }

    /**
     * Test that mirror definitions are properly evaluated. In particular, the first matching mirror definition
     * from the settings should win, i.e. ordering of mirror definitions matters.
     */
    public void testitFirstMatchWins()
        throws Exception
    {
        File testDir = ResourceExtractor.simpleExtractResources( getClass(), "/mng-3974" );

        Verifier verifier = newVerifier( testDir.getAbsolutePath() );
        verifier.setAutoclean( false );
        verifier.deleteArtifacts( "org.apache.maven.its.mng3974" );
        verifier.filterFile( "settings-template.xml", "settings.xml", "UTF-8", verifier.newDefaultFilterProperties() );
        verifier.addCliOption( "--settings" );
        verifier.addCliOption( "settings.xml" );
        verifier.executeGoal( "validate" );
        verifier.verifyErrorFreeLog();
        verifier.resetStreams();

        verifier.assertArtifactPresent( "org.apache.maven.its.mng3974", "a", "0.1", "jar" );
        verifier.assertArtifactPresent( "org.apache.maven.its.mng3974", "b", "0.1", "jar" );
    }

}
