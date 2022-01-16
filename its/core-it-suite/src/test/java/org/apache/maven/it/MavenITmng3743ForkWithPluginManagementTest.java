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
 * This is a test set for <a href="https://issues.apache.org/jira/browse/MNG-3743">MNG-3743</a>.
 *
 * todo Fill in a better description of what this test verifies!
 *
 * @author <a href="mailto:brianf@apache.org">Brian Fox</a>
 * @author jdcasey
 *
 */
public class MavenITmng3743ForkWithPluginManagementTest
    extends AbstractMavenIntegrationTestCase
{
    public MavenITmng3743ForkWithPluginManagementTest()
    {
        super( "(2.0.8,3.1)" ); // only test in 2.0.9+
    }

    public void testitMNG3743 ()
        throws Exception
    {
        File testDir = ResourceExtractor.simpleExtractResources( getClass(), "/mng-3743" );
        File pluginsDir = new File( testDir, "plugins" );
        File projectDir = new File( testDir, "project" );

        Verifier verifier = newVerifier( pluginsDir.getAbsolutePath(), "remote" );
        verifier.deleteArtifacts( "org.apache.maven.its.mng3743" );
        verifier.executeGoal( "install" );
        verifier.verifyErrorFreeLog();
        verifier.resetStreams();

        verifier = newVerifier( projectDir.getAbsolutePath(), "remote" );
        verifier.executeGoal( "site" );
        verifier.verifyErrorFreeLog();
        verifier.resetStreams();
    }
}
