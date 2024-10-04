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

import org.apache.maven.api.build.BuildContext;
import org.apache.maven.api.build.BuildContextException;
import org.apache.maven.api.build.Input;
import org.apache.maven.api.build.InputSet;
import org.apache.maven.api.build.Output;
import org.apache.maven.api.build.Resource;
import org.junit.jupiter.api.Test;

class Snippets extends AbstractBuildContextTest {

    @Test
    void snippet_1to1mapping() throws IOException {
        BuildContext buildContext = newBuildContext();
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
    void snippet_Nto1aggregation() throws IOException {
        BuildContext buildContext = newBuildContext();
        Path sourceDirectory = Files.createDirectory(temp.resolve("src"));
        Path targetDirectory = Files.createDirectory(temp.resolve("out"));
        List<String> includes = Collections.emptyList();
        List<String> excludes = Collections.emptyList();
        Path outputPath = targetDirectory.resolve("output.jar");

        InputSet registeredOutput = buildContext.newInputSet();
        registeredOutput.registerInputs(sourceDirectory, includes, excludes);
        // re-create output if any the inputs were added, changed or deleted since previous build
        boolean processingRequired = registeredOutput.aggregate(outputPath, this::aggregate);
    }

    @Test
    void snippet_Nto1aggregationWithMetadata() throws IOException {
        BuildContext buildContext = newBuildContext();
        Path sourceDirectory = Files.createDirectory(temp.resolve("src"));
        Path targetDirectory = Files.createDirectory(temp.resolve("out"));
        List<String> includes = Collections.emptyList();
        List<String> excludes = Collections.emptyList();
        Path outputPath = targetDirectory.resolve("output.jar");

        InputSet registeredOutput = buildContext.newInputSet();
        registeredOutput.registerInputs(sourceDirectory, includes, excludes);
        boolean processingRequired = registeredOutput.aggregate(
                outputPath, "myStep", new HashMap<>(), this::glean, this::merge, this::write);
    }

    @Test
    void snippet_Nto1aggregationWithMetadataManual() throws IOException {
        BuildContext buildContext = newBuildContext();
        Path sourceDirectory = Files.createDirectory(temp.resolve("src"));
        Path targetDirectory = Files.createDirectory(temp.resolve("out"));
        List<String> includes = Collections.emptyList();
        List<String> excludes = Collections.emptyList();
        Path outputPath = targetDirectory.resolve("output.jar");

        InputSet registeredOutput = buildContext.newInputSet();
        registeredOutput.registerInputs(sourceDirectory, includes, excludes);
        boolean processingRequired = registeredOutput.aggregate(
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
            throw new BuildContextException(e);
        }
    }

    private void aggregate(Output output, Collection<Input> inputs) {
        try (OutputStream os = output.newOutputStream()) {
            for (Input input : inputs) {
                Files.copy(input.getPath(), os);
            }
        } catch (IOException e) {
            throw new BuildContextException(e);
        }
    }
}
