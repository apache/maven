package org.apache.maven.it;

import org.apache.maven.shared.verifier.util.ResourceExtractor;
import org.apache.maven.shared.verifier.Verifier;

import java.io.File;

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

/**
 * This is a test set for <a href="https://issues.apache.org/jira/browse/MNG-4633">MNG-4633</a>.
 *
 *
 * @author Kristian Rosenvold
 */
public class MavenITmng4633DualCompilerExecutionsWeaveModeTest
    extends AbstractMavenIntegrationTestCase
{

    public MavenITmng4633DualCompilerExecutionsWeaveModeTest()
    {
        super( "[3.0-beta-2,)" );
    }

    /**
     * Submodule2 depends on compiler output from submodule1, but dependency is in generate-resources phase in
     * submodule2. This effectively tests the module-locking of the project artifact.
     *
     * @throws Exception in case of failure
     */
    public void testit()
        throws Exception
    {
        File testDir = ResourceExtractor.simpleExtractResources( getClass(), "/mng-4633" );

        Verifier verifier = newVerifier( testDir.getAbsolutePath(), "remote" );
        verifier.setAutoclean( false );
        verifier.addCliOption( "-T" );
        verifier.addCliOption( "2W" );
        verifier.executeGoal( "install" );
        verifier.verifyErrorFreeLog();
        verifier.resetStreams();
    }

}
