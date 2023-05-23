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
package org.apache.maven.internal.build.impl;

import java.io.File;
import java.io.IOException;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.attribute.FileTime;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Stream;

import org.apache.maven.api.build.BuildContextException;
import org.apache.maven.api.build.Metadata;
import org.apache.maven.api.build.Status;
import org.apache.maven.api.build.spi.FileState;
import org.apache.maven.api.build.spi.Workspace;
import org.codehaus.plexus.util.io.CachingOutputStream;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class DeltaWorkspaceTest extends AbstractBuildContextTest {

    public static void touch(Path path) throws InterruptedException {
        if (!Files.isRegularFile(path)) {
            throw new IllegalArgumentException("Not a file " + path);
        } else {
            File file = path.toFile();
            long lastModified = file.lastModified();
            file.setLastModified(System.currentTimeMillis());
            if (lastModified == file.lastModified()) {
                Thread.sleep(1000L);
                file.setLastModified(System.currentTimeMillis());
            }
        }
    }

    @Test
    public void testGetRegisteredInputs() throws Exception {
        DeltaWorkspace workspace;
        TestBuildContext ctx;

        // initial build
        ctx = newBuildContext();
        Path basedir = Files.createDirectory(temp.resolve("basedir"));
        Path a = Files.createFile(temp.resolve("basedir/a"));
        assertEquals(
                1, toList(ctx.registerAndProcessInputs(basedir, null, null)).size());
        assertEquals(1, toList(ctx.getRegisteredInputs()).size());
        ctx.commit();

        // no change rebuild
        workspace = new DeltaWorkspace();
        ctx = newBuildContext(workspace);
        assertEquals(
                1, toList(ctx.registerAndProcessInputs(basedir, null, null)).size());
        assertEquals(1, toList(ctx.getRegisteredInputs()).size());
        assertEquals(0, toList(ctx.getProcessedResources()).size());
        ctx.commit();

        // add input
        workspace = new DeltaWorkspace();
        Path b = Files.createFile(temp.resolve("basedir/b"));
        workspace.added.add(DefaultBuildContext.normalize(b));
        ctx = newBuildContext(workspace);
        assertEquals(
                2, toList(ctx.registerAndProcessInputs(basedir, null, null)).size());
        assertEquals(2, toList(ctx.getRegisteredInputs()).size());
        assertEquals(1, toList(ctx.getProcessedResources()).size());
        ctx.commit();

        // modify input
        workspace = new DeltaWorkspace();
        workspace.modified.add(DefaultBuildContext.normalize(a));
        ctx = newBuildContext(workspace);
        assertEquals(
                2, toList(ctx.registerAndProcessInputs(basedir, null, null)).size());
        assertEquals(2, toList(ctx.getRegisteredInputs()).size());
        assertEquals(1, toList(ctx.getProcessedResources()).size());
        ctx.commit();

        // remove input
        workspace = new DeltaWorkspace();
        Files.delete(a);
        workspace.removed.add(DefaultBuildContext.normalize(a));
        ctx = newBuildContext(workspace);
        assertEquals(
                2, toList(ctx.registerAndProcessInputs(basedir, null, null)).size());
        assertEquals(2, toList(ctx.getRegisteredInputs()).size());
        assertEquals(0, toList(ctx.getProcessedResources()).size());
        ctx.commit();
    }

    @Test
    public void testResourceStatus() throws Exception {
        Path basedir = Files.createDirectory(temp.resolve("basedir"));

        DeltaWorkspace workspace;
        TestBuildContext ctx;

        // initial build
        newBuildContext().commit();

        // new input
        workspace = new DeltaWorkspace();
        Path a = DefaultBuildContext.normalize(Files.createFile(temp.resolve("basedir/a")));
        workspace.added.add(a);
        ctx = newBuildContext(workspace);
        Metadata input = only(ctx.registerInputs(basedir, null, null));
        assertEquals(Status.NEW, input.getStatus());
        input.process();
        ctx.commit();

        // no-change rebuild
        workspace = new DeltaWorkspace();
        ctx = newBuildContext(workspace);
        input = only(ctx.registerInputs(basedir, null, null));
        assertEquals(Status.UNMODIFIED, input.getStatus());
        ctx.commit();

        // modified input
        workspace = new DeltaWorkspace();
        touch(a);
        workspace.modified.add(a);
        ctx = newBuildContext(workspace);
        input = only(ctx.registerInputs(basedir, null, null));
        assertEquals(Status.MODIFIED, input.getStatus());
        input.process();
        ctx.commit();

        // removed input
        workspace = new DeltaWorkspace();
        Files.delete(a);
        workspace.removed.add(a);
        ctx = newBuildContext(workspace);
        assertEquals(1, toList(ctx.registerInputs(basedir, null, null)).size());
        assertEquals(Status.REMOVED, ctx.getResourceStatus(a));
        input.process();
        ctx.commit();
    }

    @Test
    public void testCarryOverAndCleanup() throws Exception {
        Path basedir = Files.createDirectory(temp.resolve("basedir"));
        Path inputdir = Files.createDirectory(temp.resolve("basedir/inputdir"));
        Path outputdir = Files.createDirectory(temp.resolve("basedir/outputdir"));
        Path file = Files.createFile(temp.resolve("basedir/inputdir/file.txt"));

        DeltaWorkspace workspace;
        TestBuildContext ctx;
        Collection<? extends DefaultInput> inputs;

        // initial build
        ctx = newBuildContext();
        inputs = ctx.registerAndProcessInputs(inputdir, null, null);
        assertEquals(1, inputs.size());
        inputs.iterator().next().associateOutput(Files.createFile(temp.resolve("basedir/outputdir/file.out")));
        ctx.commit();

        // no-change rebuild
        workspace = new DeltaWorkspace();
        ctx = newBuildContext(workspace);
        ctx.registerAndProcessInputs(inputdir, null, null);
        ctx.commit();
        assertTrue(Files.exists(outputdir.resolve("file.out")));

        // "delete" input
        workspace = new DeltaWorkspace();
        workspace.removed.add(DefaultBuildContext.normalize(file));
        ctx = newBuildContext(workspace);
        ctx.registerAndProcessInputs(inputdir, null, null);
        ctx.commit();
        assertFalse(Files.exists(outputdir.resolve("file.out")));
    }

    private <T> T only(Iterable<T> values) {
        List<T> list = toList(values);
        assertEquals(1, list.size());
        return list.get(0);
    }

    private static class DeltaWorkspace implements Workspace {

        public final Set<Path> added = new HashSet<>();
        public final Set<Path> modified = new HashSet<>();
        public final Set<Path> removed = new HashSet<>();

        @Override
        public Mode getMode() {
            return Mode.DELTA;
        }

        @Override
        public Workspace escalate() {
            return new FilesystemWorkspace() {
                @Override
                public Mode getMode() {
                    return Mode.ESCALATED;
                }
            };
        }

        @Override
        public boolean isPresent(Path file) {
            return Files.exists(file);
        }

        @Override
        public boolean isRegularFile(Path file) {
            return Files.isRegularFile(file);
        }

        @Override
        public boolean isDirectory(Path file) {
            return Files.isDirectory(file);
        }

        @Override
        public void deleteFile(Path file) {
            try {
                if (Files.exists(file)) {
                    Files.delete(file);
                }
            } catch (IOException e) {
                throw new BuildContextException(e);
            }
        }

        @Override
        public void processOutput(Path path) {}

        @Override
        public OutputStream newOutputStream(Path path) {
            try {
                return new CachingOutputStream(path);
            } catch (IOException e) {
                throw new BuildContextException(e);
            }
        }

        @Override
        public Status getResourceStatus(Path file, FileTime lastModified, long size) {
            // delta workspace returns resource status compared to the previous build
            if (added.contains(file)) {
                return Status.NEW;
            }
            if (modified.contains(file)) {
                return Status.MODIFIED;
            }
            if (removed.contains(file)) {
                return Status.REMOVED;
            }
            return Status.UNMODIFIED;
        }

        @Override
        public Stream<org.apache.maven.api.build.spi.FileState> walk(Path basedir) {
            return Stream.concat(
                    Stream.concat(
                            added.stream().map(p -> new FileState(p, Status.NEW)),
                            modified.stream().map(p -> new FileState(p, Status.MODIFIED))),
                    removed.stream().map(p -> new FileState(p, Status.REMOVED)));
        }
    }
}
