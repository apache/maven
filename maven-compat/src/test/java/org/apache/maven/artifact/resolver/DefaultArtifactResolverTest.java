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
package org.apache.maven.artifact.resolver;

import java.util.Collections;

import org.apache.maven.artifact.AbstractArtifactComponentTestCase;
import org.apache.maven.artifact.Artifact;
import org.apache.maven.artifact.resolver.DefaultArtifactResolver.DaemonThreadCreator;

public class DefaultArtifactResolverTest extends AbstractArtifactComponentTestCase {
    private DefaultArtifactResolver artifactResolver;

    private Artifact projectArtifact;

    @Override
    protected void setUp() throws Exception {
        super.setUp();

        artifactResolver = (DefaultArtifactResolver) lookup(ArtifactResolver.class);

        projectArtifact = createLocalArtifact("project", "3.0");
    }

    @Override
    protected void tearDown() throws Exception {
        artifactFactory = null;
        projectArtifact = null;
        super.tearDown();
    }

    @Override
    protected String component() {
        return "resolver";
    }

    public void testMNG4738() throws Exception {
        Artifact g = createLocalArtifact("g", "1.0");
        createLocalArtifact("h", "1.0");
        artifactResolver.resolveTransitively(
                Collections.singleton(g), projectArtifact, remoteRepositories(), localRepository(), null);

        // we want to see all top-level thread groups
        ThreadGroup tg = Thread.currentThread().getThreadGroup();
        while (tg.getParent() == null) {
            tg = tg.getParent();
        }

        ThreadGroup[] tgList = new ThreadGroup[tg.activeGroupCount()];
        tg.enumerate(tgList);

        boolean seen = false;

        for (ThreadGroup aTgList : tgList) {
            if (!aTgList.getName().equals(DaemonThreadCreator.THREADGROUP_NAME)) {
                continue;
            }

            seen = true;

            tg = aTgList;
            Thread[] ts = new Thread[tg.activeCount()];
            tg.enumerate(ts);

            for (Thread active : ts) {
                String name = active.getName();
                boolean daemon = active.isDaemon();
                assertTrue(name + " is no daemon Thread.", daemon);
            }
        }

        assertTrue("Could not find ThreadGroup: " + DaemonThreadCreator.THREADGROUP_NAME, seen);
    }

    public void testLookup() throws Exception {
        ArtifactResolver resolver = lookup(ArtifactResolver.class, "default");
    }
}
