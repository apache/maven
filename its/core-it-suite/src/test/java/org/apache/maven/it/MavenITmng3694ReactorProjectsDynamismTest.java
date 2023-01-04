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

/**
 * This is a test set for <a href="https://issues.apache.org/jira/browse/MNG-3694">MNG-3694</a>:
 * Verify that any plugin injecting reactorProjects gets project instances that
 * have their concrete state calculated.
 *
 * @author jdcasey
 */
public class MavenITmng3694ReactorProjectsDynamismTest
    extends AbstractMavenIntegrationTestCase
{
    public MavenITmng3694ReactorProjectsDynamismTest()
    {
        super( ALL_MAVEN_VERSIONS );
    }

    @Test
    public void testitMNG3694 ()
        throws Exception
    {
        File testDir = ResourceExtractor.simpleExtractResources( getClass(), "/mng-3694" );

        File pluginDir = new File( testDir, "maven-mng3694-plugin" );
        File projectDir = new File( testDir, "projects" );

        Verifier verifier = newVerifier( pluginDir.getAbsolutePath(), "remote" );

        verifier.addCliArgument( "install" );
        verifier.execute();
        verifier.verifyErrorFreeLog();

        verifier = newVerifier( projectDir.getAbsolutePath() );

        verifier.addCliArgument( "validate" );
        verifier.execute();
        verifier.verifyErrorFreeLog();
    }
}
