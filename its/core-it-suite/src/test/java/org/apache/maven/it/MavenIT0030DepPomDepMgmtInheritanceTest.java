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

public class MavenIT0030DepPomDepMgmtInheritanceTest
    extends AbstractMavenIntegrationTestCase
{
    public MavenIT0030DepPomDepMgmtInheritanceTest()
    {
        super( ALL_MAVEN_VERSIONS );
    }

    /**
     * Test for injection of dependencyManagement through parents of
     * dependency poms.
     *
     * @throws Exception in case of failure
     */
    @Test
    public void testit0030()
        throws Exception
    {
        File testDir = ResourceExtractor.simpleExtractResources( getClass(), "/it0030" );
        Verifier verifier = newVerifier( testDir.getAbsolutePath(), "remote" );
        verifier.deleteArtifact( "org.apache.maven.it", "maven-it-it0030", "1.0-SNAPSHOT", "jar" );
        verifier.deleteArtifact( "org.apache.maven.it", "maven-it-it0030-child-hierarchy", "1.0-SNAPSHOT", "jar" );
        verifier.deleteArtifact( "org.apache.maven.it", "maven-it-it0030-child-project1", "1.0-SNAPSHOT", "jar" );
        verifier.deleteArtifact( "org.apache.maven.it", "maven-it-it0030-child-project2", "1.0-SNAPSHOT", "jar" );
        verifier.addCliArgument( "install" );
        verifier.execute();
        verifier.verifyFilePresent( "child-hierarchy/project2/target/classes/org/apache/maven/it0001/Person.class" );
        verifier.verifyErrorFreeLog();

    }
}

