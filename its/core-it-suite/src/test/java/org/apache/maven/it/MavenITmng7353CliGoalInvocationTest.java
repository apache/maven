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
 * Tests new <code>mvn prefix:version:goal</code>,
 * <a href="https://issues.apache.org/jira/browse/MNG-7353">MNG-7353</a>.
 */
public class MavenITmng7353CliGoalInvocationTest
    extends AbstractMavenIntegrationTestCase
{
    public MavenITmng7353CliGoalInvocationTest()
    {
        super( "[3.9.0,)" );
    }

    private void run( String id, String goal, String expectedInvocation )
        throws Exception
    {
        File basedir = ResourceExtractor.simpleExtractResources( getClass(), "/mng-7353-cli-goal-invocation" );
        Verifier verifier = newVerifier( basedir.getAbsolutePath() );
        verifier.setLogFileName( id + ".txt" );
        verifier.executeGoal( goal );
        verifier.verifyTextInLog( "[INFO] --- " + expectedInvocation );
        verifier.resetStreams();
    }

    public void testPrefixGoal()
        throws Exception
    {
        run( "pluginPrefix-goal", "dependency:list", "maven-dependency-plugin:2.7:list (default-cli)" );
    }

    public void testPrefixGoalAtId()
        throws Exception
    {
        run( "pluginPrefix-goal@id", "dependency:list@id", "maven-dependency-plugin:2.7:list (id)" );
    }

    /**
     * new pluginPrefix:version:goal in Maven 3.9.0
     */
    public void testPrefixVersionGoal()
        throws Exception
    {
        run( "pluginPrefix-version-goal", "dependency:2.8:list", "maven-dependency-plugin:2.8:list (default-cli)" );
    }

    /**
     * new pluginPrefix:version:goal in Maven 3.9.0
     */
    public void testPrefixVersionGoalAtId()
        throws Exception
    {
        run( "pluginPrefix-goal@id", "dependency:2.8:list@id", "maven-dependency-plugin:2.8:list (id)" );
    }

    public void testGroupIdArtifactIdGoal()
        throws Exception
    {
        run( "groupId-artifactId-goal", "org.apache.maven.plugins:maven-dependency-plugin:list", "maven-dependency-plugin:2.7:list (default-cli)" );
    }

    public void testGroupIdArtifactIdGoalAtId()
        throws Exception
    {
        run( "groupId-artifactId-goal@id", "org.apache.maven.plugins:maven-dependency-plugin:list@id", "maven-dependency-plugin:2.7:list (id)" );
    }

    public void testGroupIdArtifactIdVersionGoal()
        throws Exception
    {
        run( "groupId-artifactId-version-goal", "org.apache.maven.plugins:maven-dependency-plugin:2.8:list", "maven-dependency-plugin:2.8:list (default-cli)" );
    }

    public void testGroupIdArtifactIdVersionGoalAtId()
        throws Exception
    {
        run( "groupId-artifactId-version-goal@id", "org.apache.maven.plugins:maven-dependency-plugin:2.8:list@id", "maven-dependency-plugin:2.8:list (id)" );
    }
}
