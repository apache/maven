package org.apache.maven.artifact.resolver;

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

import java.util.Collections;

import org.apache.maven.artifact.AbstractArtifactComponentTestCase;
import org.apache.maven.artifact.Artifact;
import org.apache.maven.artifact.resolver.DefaultArtifactResolver.DaemonThreadCreator;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertTrue;

import javax.inject.Inject;

public class DefaultArtifactResolverTest
    extends AbstractArtifactComponentTestCase
{
    @Inject
    private ArtifactResolver artifactResolver;

    private Artifact projectArtifact;

    @BeforeEach
    @Override
    public void setUp()
        throws Exception
    {
        super.setUp();
        projectArtifact = createLocalArtifact( "project", "3.0" );
    }

    @Override
    protected String component()
    {
        return "resolver";
    }

    @Test
    public void testMNG4738()
        throws Exception
    {
        Artifact g = createLocalArtifact( "g", "1.0" );
        createLocalArtifact( "h", "1.0" );
        artifactResolver.resolveTransitively( Collections.singleton( g ), projectArtifact, remoteRepositories(),
                                              localRepository(), null );

        // we want to see all top-level thread groups
        ThreadGroup tg = Thread.currentThread().getThreadGroup();
        while ( tg.getParent() == null )
        {
            tg = tg.getParent();
        }

        ThreadGroup[] tgList = new ThreadGroup[tg.activeGroupCount()];
        tg.enumerate( tgList );

        boolean seen = false;

        for ( ThreadGroup aTgList : tgList )
        {
            if ( !aTgList.getName().equals( DaemonThreadCreator.THREADGROUP_NAME ) )
            {
                continue;
            }

            seen = true;

            tg = aTgList;
            Thread[] ts = new Thread[tg.activeCount()];
            tg.enumerate( ts );

            for ( Thread active : ts )
            {
                String name = active.getName();
                boolean daemon = active.isDaemon();
                assertTrue( daemon, name + " is no daemon Thread." );
            }

        }

        assertTrue( seen, "Could not find ThreadGroup: " + DaemonThreadCreator.THREADGROUP_NAME );
    }

    @Test
    public void testLookup()
        throws Exception
    {
        ArtifactResolver resolver = getContainer().lookup( ArtifactResolver.class, "default" );
    }
}
