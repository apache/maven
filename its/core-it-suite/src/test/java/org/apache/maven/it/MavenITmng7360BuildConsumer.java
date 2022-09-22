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
 * This test suite tests whether the consumer pom feature can load a project with a {@code parent} tag 
 * inside the plugin configuration.
 * Related JIRA issue: <a href="https://issues.apache.org/jira/browse/MNG-7360">MNG-7360</a>.
 *
 * @author Guillaume Nodet
 */
public class MavenITmng7360BuildConsumer extends AbstractMavenIntegrationTestCase
{

    private static final String PROJECT_PATH = "/mng-7360-build-consumer";

    public MavenITmng7360BuildConsumer()
    {
        super( "[4.0.0-alpha-1,)" );
    }

    public void testSelectModuleByCoordinate() throws Exception
    {
        final File projectDir = ResourceExtractor.simpleExtractResources( getClass(), PROJECT_PATH );
        final Verifier verifier = newVerifier( projectDir.getAbsolutePath() );
        verifier.executeGoal( "validate" );
        verifier.verifyErrorFreeLog();
    }

}
