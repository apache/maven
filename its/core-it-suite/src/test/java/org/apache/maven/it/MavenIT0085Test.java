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

import org.apache.maven.it.Verifier;
import org.apache.maven.it.util.ResourceExtractor;

import java.io.File;

public class MavenIT0085Test
    extends AbstractMavenIntegrationTestCase
{

    /**
     * Verify that system-scoped dependencies get resolved with system scope
     * when they are resolved transitively via another (non-system)
     * dependency. Inherited scope should not apply in the case of
     * system-scoped dependencies, no matter where they are.
     */
    public void testit0085()
        throws Exception
    {
        File testDir = ResourceExtractor.simpleExtractResources( getClass(), "/it0085" );
        Verifier verifier = new Verifier( testDir.getAbsolutePath() );
        verifier.executeGoal( "package" );
        verifier.assertFileNotPresent( "war/target/war-1.0/WEB-INF/lib/pom.xml" );
        verifier.assertFileNotPresent( "war/target/war-1.0/WEB-INF/lib/it0085-dep-1.0.jar" );
        verifier.assertFilePresent( "war/target/war-1.0/WEB-INF/lib/junit-3.8.1.jar" );
        verifier.verifyErrorFreeLog();
        verifier.resetStreams();

    }
}

