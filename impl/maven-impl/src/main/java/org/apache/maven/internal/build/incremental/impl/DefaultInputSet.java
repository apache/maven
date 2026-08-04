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

import java.io.Serializable;
import java.nio.file.Path;
import java.util.Collection;
import java.util.LinkedHashSet;
import java.util.Set;
import java.util.function.BiConsumer;
import java.util.function.BinaryOperator;
import java.util.function.Function;

import org.apache.maven.api.build.incremental.Input;
import org.apache.maven.api.build.incremental.InputSet;
import org.apache.maven.api.build.incremental.Metadata;
import org.apache.maven.api.build.incremental.Output;

public class DefaultInputSet implements InputSet {

    private final DefaultIncrementalContext context;

    private final Set<DefaultInputMetadata> inputs = new LinkedHashSet<>();

    DefaultInputSet(DefaultIncrementalContext context) {
        this.context = context;
    }

    @Override
    public void addInput(Metadata<Input> inputMetadata) {
        if (!(inputMetadata instanceof DefaultInputMetadata)) {
            throw new IllegalArgumentException("inputMetadata is not an instance of " + DefaultInputMetadata.class);
        }
        inputs.add((DefaultInputMetadata) inputMetadata);
    }

    @Override
    public Metadata<Input> registerInput(Path inputFile) {
        DefaultInputMetadata input = context.registerInput(inputFile);
        inputs.add(input);
        return input;
    }

    @Override
    public Collection<? extends Metadata<Input>> registerInputs(
            Path basedir, Collection<String> includes, Collection<String> excludes) {
        Collection<? extends DefaultInputMetadata> newInputs = context.registerInputs(basedir, includes, excludes);
        this.inputs.addAll(newInputs);
        return newInputs;
    }

    @Override
    public boolean aggregate(Path outputFile, BiConsumer<Output, Collection<Input>> aggregator) {
        return context.aggregate(inputs, outputFile, aggregator);
    }

    @Override
    public <T extends Serializable> boolean aggregate(
            Path outputFile,
            String stepId,
            T identity,
            Function<Input, T> mapper,
            BinaryOperator<T> accumulator,
            BiConsumer<Output, T> writer) {
        return context.aggregate(inputs, outputFile, stepId, identity, mapper, accumulator, writer);
    }
}
