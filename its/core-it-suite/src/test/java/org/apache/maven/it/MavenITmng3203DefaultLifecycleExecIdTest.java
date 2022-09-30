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
 *  http://www.apache.org/licenses/LICENSE-2.0
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
 * This is a test set for <a href="https://issues.apache.org/jira/browse/MNG-3203">MNG-3203</a>.
 *
 * todo Fill in a better description of what this test verifies!
 *
 * @author <a href="mailto:brianf@apache.org">Brian Fox</a>
 * @author jdcasey
 *
 */
public class MavenITmng3203DefaultLifecycleExecIdTest
    extends AbstractMavenIntegrationTestCase
{

    public MavenITmng3203DefaultLifecycleExecIdTest()
    {
        super( "[2.2.0,)" );
    }

    @Test
    public void testitMNG3203 ()
        throws Exception
    {
        // The testdir is computed from the location of this
        // file.
        File testDir = ResourceExtractor.simpleExtractResources( getClass(), "/mng-3203" );

        Verifier verifier = newVerifier( testDir.getAbsolutePath() );

        verifier.executeGoal( "compile" );
        verifier.verifyErrorFreeLog();
        verifier.resetStreams();
    }
}
