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
 * This is a test set for <a href="http://jira.codehaus.org/browse/MNG-505">MNG-505</a>.
 * 
 * @author Brett Porter
 * @version $Id$
 */
public class MavenITmng0505VersionRangeTest
    extends AbstractMavenIntegrationTestCase
{

    /**
     * Test version range junit [3.7,) resolves to 3.8.1
     */
    public void testitMNG505()
        throws Exception
    {
        File testDir = ResourceExtractor.simpleExtractResources( getClass(), "/mng-0505" );
        Verifier verifier = new Verifier( testDir.getAbsolutePath() );
        verifier.deleteArtifact( "org.apache.maven", "maven-core-it-support", "1.4", "jar" );
        verifier.deleteArtifact( "junit", "junit", "3.8", "jar" );
        verifier.executeGoal( "package" );
        verifier.assertArtifactPresent( "junit", "junit", "3.8", "jar" );
        verifier.assertArtifactPresent( "org.apache.maven", "maven-core-it-support", "1.4", "jar" );
        verifier.verifyErrorFreeLog();
        verifier.resetStreams();

    }
}

