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
 * This is a test set for <a href="http://jira.codehaus.org/browse/MNG-3920">MNG-3920</a>. The first test verifies that
 * a plugin using plexus-component-api in a Plexus component does not cause linkage errors. 
 */
public class MavenITmng3920PlexusComponentApiTest
    extends AbstractMavenIntegrationTestCase
{
    public MavenITmng3920PlexusComponentApiTest()
    {
        super( "(2.0.10,2.1.0-M1),(2.1.0-M1,)" ); // 2.0.11+, 2.1.0-M2+
    }

    public void testitMNG3920()
        throws Exception
    {
        File dir =
            ResourceExtractor.simpleExtractResources( getClass(), "/mng-3920" );

        Verifier verifier;

        verifier = newVerifier( dir.getAbsolutePath() );

        verifier.executeGoal( "validate" );

        verifier.verifyErrorFreeLog();

        verifier.resetStreams();
    }
}
