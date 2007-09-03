package org.apache.maven.integrationtests;

/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *  http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */

import java.io.File;

import org.apache.maven.it.Verifier;
import org.apache.maven.it.util.ResourceExtractor;

public class MavenIT0120EjbClientDependency
    extends AbstractMavenIntegrationTestCase
{
    public void testit0120()
        throws Exception
    {
        File testDir = ResourceExtractor.simpleExtractResources( getClass(), "/it0120-ejbClientDependency" );

        Verifier verifier = new Verifier( testDir.getAbsolutePath() );
        verifier.deleteArtifact( "org.apache.maven.its.it0120", "parent", "1.0-SNAPSHOT", "pom" );
        verifier.deleteArtifact( "org.apache.maven.its.it0120", "client", "1.0-SNAPSHOT", "jar" );
        verifier.deleteArtifact( "org.apache.maven.its.it0120", "model", "1.0-SNAPSHOT", "ejb" );

	/* Not "install" or "higher" goal to repeat the bug */
        verifier.executeGoal( "compile" ); 

	verifier.verifyErrorFreeLog();
        verifier.resetStreams();

    }
}
