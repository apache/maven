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

package org.apache.maven.it;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

import org.apache.maven.artifact.versioning.InvalidVersionSpecificationException;
import org.apache.maven.it.AbstractMavenIntegrationTestCase;
import org.apache.maven.it.Verifier;
import org.apache.maven.it.util.ResourceExtractor;

/**
 * This is a test set for <a href="http://jira.codehaus.org/browse/MNG-2720">MNG-2720</a>.
 * 
 * This test will ensure that running the 'package' phase on a multimodule build with child
 * interdependency will result in one child using the JAR of the other child in its compile
 * classpath, NOT the target/classes directory. This is critical, since sibling projects might
 * use literally ANY artifact produced by another module project, and limiting to target/classes
 * and target/test-classes eliminates many of the options that would be possible if the dependent
 * sibling were built on its own.
 *
 * @author jdcasey
 * 
 */
public class MavenITmng2720SiblingClasspathArtifactsTest
    extends AbstractMavenIntegrationTestCase
{
    public MavenITmng2720SiblingClasspathArtifactsTest()
        throws InvalidVersionSpecificationException
    {
        super( "(2.0.99,2.99.99)" );
    }

    public void testIT ()
        throws Exception
    {
        File testDir = ResourceExtractor.simpleExtractResources( getClass(), "/mng-2720" );
        File pluginDir = new File( testDir, "plugin" );
        File projectDir = new File( testDir, "project-hierarchy" );

        // First, install the plugin used for the test.
        Verifier verifier = new Verifier( pluginDir.getAbsolutePath() );
        verifier.executeGoal( "install" );
        verifier.verifyErrorFreeLog();
        verifier.resetStreams();
        
        // Now, build the project hierarchy that uses the plugin to verify sibling dependencies.
        verifier = new Verifier( projectDir.getAbsolutePath() );
        verifier.executeGoal( "package" );
        verifier.verifyErrorFreeLog();
    }
}
