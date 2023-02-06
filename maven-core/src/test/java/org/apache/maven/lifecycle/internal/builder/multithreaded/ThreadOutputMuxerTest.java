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
package org.apache.maven.lifecycle.internal.builder.multithreaded;

import java.io.ByteArrayOutputStream;
import java.io.PrintStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Iterator;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.CompletionService;
import java.util.concurrent.ExecutorCompletionService;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

import junit.framework.TestCase;
import org.apache.maven.execution.MavenSession;
import org.apache.maven.lifecycle.LifecycleNotFoundException;
import org.apache.maven.lifecycle.LifecyclePhaseNotFoundException;
import org.apache.maven.lifecycle.internal.ProjectBuildList;
import org.apache.maven.lifecycle.internal.ProjectSegment;
import org.apache.maven.lifecycle.internal.stub.ProjectDependencyGraphStub;
import org.apache.maven.plugin.InvalidPluginDescriptorException;
import org.apache.maven.plugin.MojoNotFoundException;
import org.apache.maven.plugin.PluginDescriptorParsingException;
import org.apache.maven.plugin.PluginNotFoundException;
import org.apache.maven.plugin.PluginResolutionException;
import org.apache.maven.plugin.prefix.NoPluginFoundForPrefixException;
import org.apache.maven.plugin.version.PluginVersionResolutionException;

/**
 * @author Kristian Rosenvold
 */
public class ThreadOutputMuxerTest extends TestCase {

    final String paid = "Paid";

    final String in = "In";

    final String full = "Full";

    public void testSingleThreaded() throws Exception {
        ProjectBuildList src = getProjectBuildList();
        ProjectBuildList projectBuildList = new ProjectBuildList(Arrays.asList(src.get(0), src.get(1), src.get(2)));

        ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();
        PrintStream systemOut = new PrintStream(byteArrayOutputStream);
        ThreadOutputMuxer threadOutputMuxer = new ThreadOutputMuxer(projectBuildList, systemOut);

        threadOutputMuxer.associateThreadWithProjectSegment(projectBuildList.get(0));
        System.out.print(paid); // No, this does not print to system.out. It's part of the test
        assertEquals(paid.length(), byteArrayOutputStream.size());
        threadOutputMuxer.associateThreadWithProjectSegment(projectBuildList.get(1));
        System.out.print(in); // No, this does not print to system.out. It's part of the test
        assertEquals(paid.length(), byteArrayOutputStream.size());
        threadOutputMuxer.associateThreadWithProjectSegment(projectBuildList.get(2));
        System.out.print(full); // No, this does not print to system.out. It's part of the test
        assertEquals(paid.length(), byteArrayOutputStream.size());

        threadOutputMuxer.setThisModuleComplete(projectBuildList.get(0));
        threadOutputMuxer.setThisModuleComplete(projectBuildList.get(1));
        threadOutputMuxer.setThisModuleComplete(projectBuildList.get(2));
        threadOutputMuxer.close();
        assertEquals((paid + in + full).length(), byteArrayOutputStream.size());
    }

    public void testMultiThreaded() throws Exception {
        ProjectBuildList projectBuildList = getProjectBuildList();

        ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();
        PrintStream systemOut = new PrintStream(byteArrayOutputStream);
        final ThreadOutputMuxer threadOutputMuxer = new ThreadOutputMuxer(projectBuildList, systemOut);

        final List<String> stringList = Arrays.asList(
                "Thinkin", "of", "a", "master", "plan", "Cuz", "ain’t", "nuthin", "but", "sweat", "inside", "my",
                "hand");
        Iterator<String> lyrics = stringList.iterator();

        ExecutorService executor = Executors.newFixedThreadPool(10);
        CompletionService<ProjectSegment> service = new ExecutorCompletionService<>(executor);

        List<Future<ProjectSegment>> futures = new ArrayList<>();
        for (ProjectSegment projectBuild : projectBuildList) {
            final Future<ProjectSegment> buildFuture =
                    service.submit(new Outputter(threadOutputMuxer, projectBuild, lyrics.next()));
            futures.add(buildFuture);
        }

        for (Future<ProjectSegment> future : futures) {
            future.get();
        }
        int expectedLength = 0;
        for (int i = 0; i < projectBuildList.size(); i++) {
            expectedLength += stringList.get(i).length();
        }

        threadOutputMuxer.close();
        final byte[] bytes = byteArrayOutputStream.toByteArray();
        String result = new String(bytes);
        assertEquals(result, expectedLength, bytes.length);
    }

    class Outputter implements Callable<ProjectSegment> {
        private final ThreadOutputMuxer threadOutputMuxer;

        private final ProjectSegment item;

        private final String response;

        Outputter(ThreadOutputMuxer threadOutputMuxer, ProjectSegment item, String response) {
            this.threadOutputMuxer = threadOutputMuxer;
            this.item = item;
            this.response = response;
        }

        public ProjectSegment call() throws Exception {
            threadOutputMuxer.associateThreadWithProjectSegment(item);
            System.out.print(response);
            threadOutputMuxer.setThisModuleComplete(item);
            return item;
        }
    }

    private ProjectBuildList getProjectBuildList()
            throws InvalidPluginDescriptorException, PluginVersionResolutionException, PluginDescriptorParsingException,
                    NoPluginFoundForPrefixException, MojoNotFoundException, PluginNotFoundException,
                    PluginResolutionException, LifecyclePhaseNotFoundException, LifecycleNotFoundException {
        final MavenSession session = ProjectDependencyGraphStub.getMavenSession();
        return ProjectDependencyGraphStub.getProjectBuildList(session);
    }
}
