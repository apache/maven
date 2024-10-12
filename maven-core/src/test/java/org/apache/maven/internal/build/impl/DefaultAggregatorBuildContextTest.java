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

import java.io.BufferedWriter;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.function.BiConsumer;

import org.apache.maven.api.build.BuildContextException;
import org.apache.maven.api.build.Input;
import org.apache.maven.api.build.Output;
import org.apache.maven.api.build.Resource;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class DefaultAggregatorBuildContextTest extends AbstractBuildContextTest {

    @Test
    public void testBasic() throws Exception {
        FileMatcher.getCanonicalPath(Paths.get("/oo/bar"));

        Path outputFile = temp.resolve("output");

        Path basedir = Files.createTempDirectory(temp, "").toRealPath();
        Path a = basedir.resolve("a");
        Files.createFile(a);

        // initial build
        FileIndexer indexer = new FileIndexer();
        DefaultBuildContext actx = newContext();
        DefaultInputSet inputSet = actx.newInputSet();
        inputSet.registerInputs(basedir, null, null);
        inputSet.aggregate(outputFile, indexer);
        actx.commit(null);
        assertTrue(Files.isReadable(outputFile));
        assertEquals(1, indexer.outputs.size());
        assertEquals(1, indexer.inputs.size());

        // no-change rebuild
        indexer = new FileIndexer();
        actx = newContext();
        inputSet = actx.newInputSet();
        inputSet.registerInputs(basedir, null, null);
        inputSet.aggregate(outputFile, indexer);
        actx.commit(null);
        assertTrue(Files.isReadable(outputFile));
        assertEquals(0, indexer.outputs.size());

        // no-change rebuild
        indexer = new FileIndexer();
        actx = newContext();

        inputSet = actx.newInputSet();
        inputSet.registerInputs(basedir, null, null);
        inputSet.aggregate(outputFile, indexer);
        actx.commit(null);
        assertTrue(Files.isReadable(outputFile));
        assertEquals(0, indexer.outputs.size());

        // new input
        Path b = basedir.resolve("b");
        Files.createFile(b);
        indexer = new FileIndexer();
        actx = newContext();
        inputSet = actx.newInputSet();
        inputSet.registerInputs(basedir, null, null);
        inputSet.aggregate(outputFile, indexer);
        actx.commit(null);
        assertTrue(Files.isReadable(outputFile));
        assertEquals(1, indexer.outputs.size());

        // removed input
        Files.delete(a);
        indexer = new FileIndexer();
        actx = newContext();
        inputSet = actx.newInputSet();
        inputSet.registerInputs(basedir, null, null);
        inputSet.aggregate(outputFile, indexer);
        actx.commit(null);
        assertTrue(Files.isReadable(outputFile));
        assertEquals(1, indexer.outputs.size());

        // no-change rebuild
        indexer = new FileIndexer();
        actx = newContext();
        inputSet = actx.newInputSet();
        inputSet.registerInputs(basedir, null, null);
        inputSet.aggregate(outputFile, indexer);
        actx.commit(null);
        assertTrue(Files.isReadable(outputFile));
        assertEquals(0, indexer.outputs.size());

        // removed output
        Files.delete(outputFile);
        indexer = new FileIndexer();
        actx = newContext();
        inputSet = actx.newInputSet();
        inputSet.registerInputs(basedir, null, null);
        inputSet.aggregate(outputFile, indexer);
        actx.commit(null);
        assertTrue(Files.isReadable(outputFile));
        assertEquals(1, indexer.outputs.size());

        // no-change rebuild
        indexer = new FileIndexer();
        actx = newContext();
        inputSet = actx.newInputSet();
        inputSet.registerInputs(basedir, null, null);
        inputSet.aggregate(outputFile, indexer);
        actx.commit(null);
        assertTrue(Files.isReadable(outputFile));
        assertEquals(0, indexer.outputs.size());
    }

    private DefaultBuildContext newContext() {
        Path stateFile = temp.resolve("buildstate.ctx");
        return new DefaultBuildContext(new FilesystemWorkspace(), stateFile, new HashMap<>(), null);
    }

    @Test
    public void testEmpty() throws Exception {
        Path outputFile = temp.resolve("output");
        Path basedir = Files.createTempDirectory(temp, "");

        FileIndexer indexer = new FileIndexer();
        DefaultBuildContext actx = newContext();
        DefaultInputSet output = actx.newInputSet();
        output.registerInputs(basedir, null, null);
        output.aggregate(outputFile, indexer);
        actx.commit(null);
        assertTrue(Files.isReadable(outputFile));
        assertEquals(1, indexer.outputs.size());
    }

    private static class FileIndexer implements BiConsumer<Output, Collection<Input>> {
        public final List<Path> inputs = new ArrayList<>();
        public final List<Path> outputs = new ArrayList<>();

        @Override
        public void accept(Output output, Collection<Input> inputs) {
            outputs.add(output.getPath());
            try (BufferedWriter w = output.newBufferedWriter(StandardCharsets.UTF_8)) {
                for (Resource input : inputs) {
                    Path path = input.getPath();
                    this.inputs.add(path);
                    w.write(path.toAbsolutePath().toString());
                    w.newLine();
                }
            } catch (IOException e) {
                throw new BuildContextException(e);
            }
        }
    }
}
