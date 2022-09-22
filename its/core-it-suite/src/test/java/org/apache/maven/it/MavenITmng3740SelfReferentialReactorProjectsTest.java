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

import org.apache.maven.shared.verifier.util.ResourceExtractor;
import org.apache.maven.shared.verifier.Verifier;

/**
 * This is a test set for <a href="https://issues.apache.org/jira/browse/MNG-3740">MNG-3740</a>.
 *
 * Check that when a plugin project build uses an earlier version of itself, it
 * doesn't result in a StackOverflowError as a result of trying to calculate
 * a concrete state for its project references, which includes itself because of
 * this plugin configuration in the POM.
 *
 * @author jdcasey
 *
 */
public class MavenITmng3740SelfReferentialReactorProjectsTest
    extends AbstractMavenIntegrationTestCase
{
    public MavenITmng3740SelfReferentialReactorProjectsTest()
    {
        super( "(2.0.8,)" ); // only test in 2.0.9+
    }

    public void testitMNG3740 ()
        throws Exception
    {
        File testDir = ResourceExtractor.simpleExtractResources( getClass(), "/mng-3740" );
        File v1 = new File( testDir, "projects.v1" );
        File v2 = new File( testDir, "projects.v2" );

        Verifier verifier;

        verifier = newVerifier( v1.getAbsolutePath(), "remote" );
        verifier.executeGoal( "install" );
        verifier.verifyErrorFreeLog();
        verifier.resetStreams();

        verifier = newVerifier( v2.getAbsolutePath() );
        verifier.executeGoal( "package" );
        verifier.verifyErrorFreeLog();
        verifier.resetStreams();

    }
}
