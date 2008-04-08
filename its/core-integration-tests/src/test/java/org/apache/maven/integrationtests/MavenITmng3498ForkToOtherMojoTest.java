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

package org.apache.maven.integrationtests;

import org.apache.maven.it.Verifier;
import org.apache.maven.it.util.ResourceExtractor;

import java.io.File;

/**
 * This is a test set for <a href="http://jira.codehaus.org/browse/MNG-3498">MNG-3498</a>.
 *
 * @todo Fill in a better description of what this test verifies!
 *
 * @author <a href="mailto:brianf@apache.org">Brian Fox</a>
 * @author jdcasey
 *
 */
public class MavenITmng3498ForkToOtherMojoTest
    extends AbstractMavenIntegrationTestCase
{
//    public MavenITmng3498ForkToOtherMojoTest()
//        throws InvalidVersionSpecificationException
//    {
//        super( "(2.0.8,)" ); // only test in 2.0.9+
//    }

    public void testitMNG3498 ()
        throws Exception
    {
        // The testdir is computed from the location of this
        // file.
        File testDir = ResourceExtractor.simpleExtractResources( getClass(), "/mng-3498" );

        File pluginDir = new File( testDir, "maven-mng3498-plugin" );
        File projectDir = new File( testDir, "mng-3498-project" );

        Verifier verifier = new Verifier( pluginDir.getAbsolutePath() );
        verifier.deleteArtifact( "org.apache.maven.its.mng3498", "maven-mng3498-plugin", "1", "pom" );

        verifier.executeGoal( "install" );

        verifier.verifyErrorFreeLog();
        verifier.resetStreams();

        verifier = new Verifier( projectDir.getAbsolutePath() );

        verifier.executeGoal( "validate" );

        verifier.verifyErrorFreeLog();
        verifier.resetStreams();
    }
}
