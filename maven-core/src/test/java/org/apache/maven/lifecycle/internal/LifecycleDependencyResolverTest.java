package org.apache.maven.lifecycle.internal;

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

import java.io.File;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.Properties;
import java.util.Set;

import org.apache.maven.AbstractCoreMavenComponentTestCase;
import org.apache.maven.artifact.Artifact;
import org.apache.maven.execution.MavenSession;
import org.apache.maven.project.MavenProject;
import org.codehaus.plexus.component.annotations.Requirement;
import org.junit.Test;

public class LifecycleDependencyResolverTest extends AbstractCoreMavenComponentTestCase
{
    @Requirement
    private LifecycleDependencyResolver resolver;

    @Override
    protected String getProjectsDirectory()
    {
        return null;
    }

    @Override
    protected void setUp()
        throws Exception
    {
        super.setUp();
        resolver = lookup( LifecycleDependencyResolver.class );
    }

    @Test
    public void testCachedReactorProjectDependencies() throws Exception
    {
        MavenSession session = createMavenSession( new File( "src/test/projects/lifecycle-dependency-resolver/pom.xml" ), new Properties(), true );
        Collection<String> scopesToCollect = null;
        Collection<String> scopesToResolve = Collections.singletonList( "compile" );
        boolean aggregating = false;

        Set<Artifact> reactorArtifacts = new HashSet<>( 3 );
        for ( MavenProject reactorProject : session.getProjects() )
        {
            reactorProject.setArtifactFilter(artifact -> true);
            resolver.resolveProjectDependencies( reactorProject, scopesToCollect, scopesToResolve, session, aggregating, reactorArtifacts );
            reactorArtifacts.add( reactorProject.getArtifact() );
        }

        MavenProject lib = session.getProjects().get( 1 );
        MavenProject war = session.getProjects().get( 2 );

        assertEquals( null , war.getArtifactMap().get("org.apache.maven.its.mng6300:mng6300-lib").getFile() );

        lib.getArtifact().setFile( new File( "lib.jar" ) );

        resolver.resolveProjectDependencies( war, scopesToCollect, scopesToResolve, session, aggregating, reactorArtifacts );

        assertEquals( new File( "lib.jar" ) , war.getArtifactMap().get("org.apache.maven.its.mng6300:mng6300-lib").getFile() );
    }
}
