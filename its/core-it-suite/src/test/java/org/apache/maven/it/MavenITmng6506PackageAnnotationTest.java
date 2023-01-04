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

public class MavenITmng6506PackageAnnotationTest
    extends AbstractMavenIntegrationTestCase
{

    public MavenITmng6506PackageAnnotationTest()
    {
        super( "[3.6.1,)" );
    }

    @Test
    public void testGetPackageAnnotation()
            throws Exception
    {
        File testDir =
                ResourceExtractor.simpleExtractResources( getClass(),
                                                          "/mng-6506-package-annotation" );
        File pluginDir = new File( testDir, "plugin" );
        File projectDir = new File( testDir, "project" );

        Verifier verifier;

        verifier = newVerifier( pluginDir.getAbsolutePath(), "remote" );
        verifier.addCliArgument( "install" );
        verifier.execute();
        verifier.verifyErrorFreeLog();

        verifier = newVerifier( projectDir.getAbsolutePath(), "remote" );
        verifier.addCliArgument( "verify" );
        verifier.execute();
        verifier.verifyErrorFreeLog();
        verifier.verifyTextInLog( "MNG-6506 check succeeded" );
    }
}
