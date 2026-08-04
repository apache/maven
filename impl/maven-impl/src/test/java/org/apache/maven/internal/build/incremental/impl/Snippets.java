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
package org.apache.maven.internal.build.incremental.impl;

import java.io.DataOutputStream;
import java.io.IOException;
import java.io.ObjectOutputStream;
import java.io.OutputStream;
import java.io.Serializable;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;

import org.apache.maven.api.build.incremental.IncrementalContext;
import org.apache.maven.api.build.incremental.IncrementalContextException;
import org.apache.maven.api.build.incremental.Input;
import org.apache.maven.api.build.incremental.InputSet;
import org.apache.maven.api.build.incremental.Output;
import org.apache.maven.api.build.incremental.Resource;
import org.junit.jupiter.api.Test;

class Snippets extends AbstractIncrementalContextTest {

    @Test
    void snippet1To1Mapping() throws IOException {
        IncrementalContext buildContext = newIncrementalContext();
        Path sourceDirectory = Files.createDirectory(temp.resolve("src"));
        Path targetDirectory = Files.createDirectory(temp.resolve("out"));
        List<String> includes = Collections.emptyList();
        List<String> excludes = Collections.emptyList();

        for (Input input : buildContext.registerAndProcessInputs(sourceDirectory, includes, excludes)) {
            Path outputPath = targetDirectory.resolve(sourceDirectory.relativize(input.getPath()));
            Output output = input.associateOutput(outputPath);
            try (OutputStream os = output.newOutputStream()) {
                Files.copy(input.getPath(), os);
            }
        }
    }

    @Test
    void snippetNTo1Aggregation() throws IOException {
        IncrementalContext buildContext = newIncrementalContext();
        Path sourceDirectory = Files.createDirectory(temp.resolve("src"));
        Path targetDirectory = Files.createDirectory(temp.resolve("out"));
        List<String> includes = Collections.emptyList();
        List<String> excludes = Collections.emptyList();
        Path outputPath = targetDirectory.resolve("output.jar");

        InputSet registeredOutput = buildContext.newInputSet();
        registeredOutput.registerInputs(sourceDirectory, includes, excludes);
        // re-create output if any the inputs were added, changed or deleted since previous build
        registeredOutput.aggregate(outputPath, this::aggregate);
    }

    @Test
    void snippetNTo1AggregationWithMetadata() throws IOException {
        IncrementalContext buildContext = newIncrementalContext();
        Path sourceDirectory = Files.createDirectory(temp.resolve("src"));
        Path targetDirectory = Files.createDirectory(temp.resolve("out"));
        List<String> includes = Collections.emptyList();
        List<String> excludes = Collections.emptyList();
        Path outputPath = targetDirectory.resolve("output.jar");

        InputSet registeredOutput = buildContext.newInputSet();
        registeredOutput.registerInputs(sourceDirectory, includes, excludes);
        registeredOutput.aggregate(outputPath, "myStep", new HashMap<>(), this::glean, this::merge, this::write);
    }

    @Test
    void snippetNTo1AggregationWithMetadataManual() throws IOException {
        IncrementalContext buildContext = newIncrementalContext();
        Path sourceDirectory = Files.createDirectory(temp.resolve("src"));
        Path targetDirectory = Files.createDirectory(temp.resolve("out"));
        List<String> includes = Collections.emptyList();
        List<String> excludes = Collections.emptyList();
        Path outputPath = targetDirectory.resolve("output.jar");

        InputSet registeredOutput = buildContext.newInputSet();
        registeredOutput.registerInputs(sourceDirectory, includes, excludes);
        registeredOutput.aggregate(
                outputPath,
                (output, inputs) ->
                        write(output, inputs.stream().map(this::glean).reduce(new HashMap<>(), this::merge)));
    }

    private HashMap<String, Serializable> glean(Resource resource) {
        HashMap<String, Serializable> m = new HashMap<>();
        m.put("name", resource.getPath().toString());
        return m;
    }

    private HashMap<String, Serializable> merge(HashMap<String, Serializable> m1, HashMap<String, Serializable> m2) {
        HashMap<String, Serializable> m = new HashMap<>(m1);
        m.putAll(m2);
        return m;
    }

    private void write(Output output, HashMap<String, Serializable> metadata) {
        try {
            try (ObjectOutputStream os = new ObjectOutputStream(new DataOutputStream(output.newOutputStream()))) {
                os.writeObject(metadata);
            }
        } catch (IOException e) {
            throw new IncrementalContextException(e);
        }
    }

    private void aggregate(Output output, Collection<Input> inputs) {
        try (OutputStream os = output.newOutputStream()) {
            for (Input input : inputs) {
                Files.copy(input.getPath(), os);
            }
        } catch (IOException e) {
            throw new IncrementalContextException(e);
        }
    }
}
