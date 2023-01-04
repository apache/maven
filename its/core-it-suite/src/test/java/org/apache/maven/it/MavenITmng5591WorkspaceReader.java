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

import org.apache.maven.shared.verifier.util.ResourceExtractor;
import org.apache.maven.shared.verifier.Verifier;

import java.io.File;

import org.junit.jupiter.api.Test;

public class MavenITmng5591WorkspaceReader
    extends AbstractMavenIntegrationTestCase
{
    public MavenITmng5591WorkspaceReader()
    {
        super( "[3.1.0,)" );
    }

    @Test
    public void testWorkspaceReader()
        throws Exception
    {
        /*
         * The point of this test is to validate that ide WorkspaceReader, like one used by m2e, does not interfere with
         * reactor dependency resolution. The test comes in two parts. mng-5591-workspace-reader/extension is noop
         * WorkspaceReader implementation and mng-5591-workspace-reader/basic is a multi-module project with inter-module
         * dependencies. The workspace reader extension is injected in maven runtime with -Dmaven.ext.class.path command
         * line argument. The multi-module build fails unless reactor resolution works properly.
         */

        File testDir = ResourceExtractor.simpleExtractResources( getClass(), "/mng-5591-workspace-reader" );
        File extensionDir = new File( testDir, "extension" );
        File projectDir = new File( testDir, "basic" );

        Verifier verifier;

        // install the test extension
        verifier = newVerifier( extensionDir.getAbsolutePath(), "remote" );
        verifier.addCliArgument( "install" );
        verifier.execute();
        verifier.verifyErrorFreeLog();

        // compile the test project
        verifier = newVerifier( projectDir.getAbsolutePath(), "remote" );
        verifier.addCliArgument( "-Dmaven.ext.class.path="
            + new File( extensionDir, "target/mng-5591-workspace-reader-extension-0.1.jar" ).getCanonicalPath() );
        verifier.addCliArgument( "compile" );
        verifier.execute();
        verifier.verifyErrorFreeLog();
    }

}
