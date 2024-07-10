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
package org.apache.maven.lifecycle.internal;

import java.util.List;

import org.apache.maven.execution.MavenSession;
import org.apache.maven.execution.ProjectDependencyGraph;
import org.apache.maven.lifecycle.LifecycleNotFoundException;
import org.apache.maven.lifecycle.LifecyclePhaseNotFoundException;
import org.apache.maven.lifecycle.internal.builder.multithreaded.ConcurrencyDependencyGraph;
import org.apache.maven.lifecycle.internal.stub.ProjectDependencyGraphStub;
import org.apache.maven.plugin.InvalidPluginDescriptorException;
import org.apache.maven.plugin.MojoNotFoundException;
import org.apache.maven.plugin.PluginDescriptorParsingException;
import org.apache.maven.plugin.PluginNotFoundException;
import org.apache.maven.plugin.PluginResolutionException;
import org.apache.maven.plugin.prefix.NoPluginFoundForPrefixException;
import org.apache.maven.plugin.version.PluginVersionResolutionException;
import org.apache.maven.project.MavenProject;

import static org.apache.maven.lifecycle.internal.stub.ProjectDependencyGraphStub.A;
import static org.apache.maven.lifecycle.internal.stub.ProjectDependencyGraphStub.B;
import static org.apache.maven.lifecycle.internal.stub.ProjectDependencyGraphStub.C;
import static org.apache.maven.lifecycle.internal.stub.ProjectDependencyGraphStub.X;
import static org.apache.maven.lifecycle.internal.stub.ProjectDependencyGraphStub.Y;
import static org.apache.maven.lifecycle.internal.stub.ProjectDependencyGraphStub.Z;
import static org.apache.maven.lifecycle.internal.stub.ProjectDependencyGraphStub.getProjectBuildList;

/**
 * @author Kristian Rosenvold
 */
public class ConcurrencyDependencyGraphTest extends junit.framework.TestCase {
    public void testConcurrencyGraphPrimaryVersion()
            throws InvalidPluginDescriptorException, PluginVersionResolutionException, PluginDescriptorParsingException,
                    NoPluginFoundForPrefixException, MojoNotFoundException, PluginNotFoundException,
                    PluginResolutionException, LifecyclePhaseNotFoundException, LifecycleNotFoundException {
        ProjectDependencyGraph dependencyGraph = new ProjectDependencyGraphStub();
        final MavenSession session = ProjectDependencyGraphStub.getMavenSession();

        ConcurrencyDependencyGraph graph =
                new ConcurrencyDependencyGraph(getProjectBuildList(session), dependencyGraph);

        final List<MavenProject> projectBuilds = graph.getRootSchedulableBuilds();
        assertEquals(1, projectBuilds.size());
        assertEquals(A, projectBuilds.iterator().next());

        final List<MavenProject> subsequent = graph.markAsFinished(A);
        assertEquals(2, subsequent.size());
        assertEquals(B, subsequent.get(0));
        assertEquals(C, subsequent.get(1));

        final List<MavenProject> bDescendants = graph.markAsFinished(B);
        assertEquals(1, bDescendants.size());
        assertEquals(Y, bDescendants.get(0));

        final List<MavenProject> cDescendants = graph.markAsFinished(C);
        assertEquals(2, cDescendants.size());
        assertEquals(X, cDescendants.get(0));
        assertEquals(Z, cDescendants.get(1));
    }

    public void testConcurrencyGraphDifferentCompletionOrder()
            throws InvalidPluginDescriptorException, PluginVersionResolutionException, PluginDescriptorParsingException,
                    NoPluginFoundForPrefixException, MojoNotFoundException, PluginNotFoundException,
                    PluginResolutionException, LifecyclePhaseNotFoundException, LifecycleNotFoundException {
        ProjectDependencyGraph dependencyGraph = new ProjectDependencyGraphStub();
        final MavenSession session = ProjectDependencyGraphStub.getMavenSession();
        ConcurrencyDependencyGraph graph =
                new ConcurrencyDependencyGraph(getProjectBuildList(session), dependencyGraph);

        graph.markAsFinished(A);
        final List<MavenProject> cDescendants = graph.markAsFinished(C);
        assertEquals(1, cDescendants.size());
        assertEquals(Z, cDescendants.get(0));

        final List<MavenProject> bDescendants = graph.markAsFinished(B);
        assertEquals(2, bDescendants.size());
        assertEquals(X, bDescendants.get(0));
        assertEquals(Y, bDescendants.get(1));
    }
}
