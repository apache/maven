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

import java.io.IOException;
import java.io.OutputStream;
import java.io.Serializable;
import java.nio.file.Files;
import java.nio.file.NoSuchFileException;
import java.nio.file.Path;
import java.nio.file.attribute.BasicFileAttributes;
import java.nio.file.attribute.FileTime;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.function.BiConsumer;
import java.util.function.BinaryOperator;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.apache.maven.api.annotations.Nullable;
import org.apache.maven.api.build.BuildContextException;
import org.apache.maven.api.build.Input;
import org.apache.maven.api.build.Metadata;
import org.apache.maven.api.build.Output;
import org.apache.maven.api.build.Severity;
import org.apache.maven.api.build.Status;
import org.apache.maven.api.build.spi.BuildContextEnvironment;
import org.apache.maven.api.build.spi.BuildContextFinalizer;
import org.apache.maven.api.build.spi.CommitableBuildContext;
import org.apache.maven.api.build.spi.Message;
import org.apache.maven.api.build.spi.Sink;
import org.apache.maven.api.build.spi.Workspace;

public class DefaultBuildContext implements CommitableBuildContext {
    final Workspace workspace;
    final Path stateFile;
    final DefaultBuildContextState state;
    final DefaultBuildContextState oldState;
    /**
     * Previous build state does not exist, cannot be read or configuration has changed. When
     * escalated, all input files are considered require processing.
     */
    private final boolean escalated;
    /**
     * Resources known to be deleted since previous build. Includes both resources reported as deleted
     * by Workspace and resources explicitly delete through this build context.
     */
    private final Set<Path> deletedResources = new HashSet<>();
    /**
     * Resources selected for processing during this build. This includes resources created, changed
     * and deleted through this build context.
     */
    private final Set<Path> processedResources = new HashSet<>();
    /**
     * Indicates that no further modifications to this build context are allowed.
     */
    private boolean closed;
    /**
     * Indicates whether the build will continue even if there are compilation errors.
     */
    private boolean failOnError = true;

    public DefaultBuildContext(BuildContextEnvironment env) {
        this(env.getWorkspace(), env.getStateFile(), env.getParameters(), env.getFinalizer());
    }

    protected DefaultBuildContext(
            Workspace workspace,
            Path stateFile,
            Map<String, Serializable> configuration,
            BuildContextFinalizer finalizer) {
        // preconditions
        if (workspace == null) {
            throw new NullPointerException();
        }
        if (configuration == null) {
            throw new NullPointerException();
        }

        this.stateFile = stateFile != null ? stateFile.toAbsolutePath() : null;
        this.state = DefaultBuildContextState.withConfiguration(configuration);
        this.oldState = DefaultBuildContextState.loadFrom(this.stateFile);

        final boolean configurationChanged = getConfigurationChanged();
        if (workspace.getMode() == Workspace.Mode.ESCALATED) {
            this.escalated = true;
            this.workspace = workspace;
        } else if (workspace.getMode() == Workspace.Mode.SUPPRESSED) {
            this.escalated = false;
            this.workspace = workspace;
        } else if (configurationChanged || !isPresent(oldState.getOutputs())) {
            this.escalated = true;
            this.workspace = workspace.escalate();
        } else {
            this.escalated = false;
            this.workspace = workspace;
        }

        if (escalated && this.stateFile != null) {
            if (!Files.isReadable(this.stateFile)) {
                logInfo("Previous incremental build state does not exist, performing full build");
            } else {
                logInfo("Incremental build configuration change detected, performing full build");
            }
        } else {
            logInfo("Performing incremental build");
        }

        if (finalizer != null) {
            finalizer.registerContext(this);
        }
    }

    private static boolean containsOnly(Collection<Path> collection, Path element) {
        return collection.stream().allMatch(element::equals);
    }

    static Path normalize(Path input) {
        if (input == null) {
            throw new IllegalArgumentException();
        }
        return FileMatcher.getCanonicalPath(input);
    }

    public boolean getFailOnError() {
        return failOnError;
    }

    @Override
    public void setFailOnError(boolean failOnError) {
        this.failOnError = failOnError;
    }

    protected void logInfo(String message) {
        System.out.println(message);
    }

    private boolean isPresent(Collection<Path> outputs) {
        // in some scenarios, notable classpath change caused by changes to pom.xml,
        // jdt builder deletes all files from target/classes directory during incremental workspace
        // build. this behaviour is not communicated to m2e (or any other workspace builder) and thus
        // m2e does not recreate deleted outputs
        // this workaround escalates the build if any of the old outputs were deleted
        return outputs.stream().allMatch(Files::isRegularFile);
    }

    private boolean getConfigurationChanged() {
        Map<String, Serializable> configuration = state.configuration;
        Map<String, Serializable> oldConfiguration = oldState.configuration;
        return Stream.concat(configuration.keySet().stream(), oldConfiguration.keySet().stream())
                .distinct()
                .anyMatch(k -> !Objects.equals(configuration.get(k), oldConfiguration.get(k)));
    }

    @Override
    public boolean isProcessingRequired() {
        return isEscalated()
                || state.getResources().keySet().stream()
                        .anyMatch(resource ->
                                !state.isOutput(resource) && getResourceStatus(resource) != Status.UNMODIFIED)
                || oldState.getResources().keySet().stream()
                        .anyMatch(resource -> !oldState.isOutput(resource) && !state.isResource(resource));
    }

    @Override
    public DefaultOutput processOutput(Path outputFile) {
        outputFile = normalize(outputFile);
        DefaultOutputMetadata metadata = registerNormalizedOutput(outputFile);
        return processOutput(metadata);
    }

    protected DefaultOutput processOutput(DefaultOutputMetadata metadata) {
        processResource(metadata.getPath());
        workspace.processOutput(metadata.getPath());
        return newOutput(metadata);
    }

    @Override
    public DefaultInputSet newInputSet() {
        return new DefaultInputSet(this);
    }

    @Override
    public DefaultInputMetadata registerInput(Path inputFile) {
        inputFile = normalize(inputFile);
        BasicFileAttributes attrs = readAttributes(inputFile);
        return registerNormalizedInput(inputFile, attrs.lastModifiedTime(), attrs.size());
    }

    static BasicFileAttributes readAttributes(Path inputFile) {
        try {
            return Files.readAttributes(inputFile, BasicFileAttributes.class);
        } catch (NoSuchFileException e) {
            return new BasicFileAttributes() {
                @Override
                public FileTime lastModifiedTime() {
                    return null;
                }

                @Override
                public FileTime lastAccessTime() {
                    return null;
                }

                @Override
                public FileTime creationTime() {
                    return null;
                }

                @Override
                public boolean isRegularFile() {
                    return false;
                }

                @Override
                public boolean isDirectory() {
                    return false;
                }

                @Override
                public boolean isSymbolicLink() {
                    return false;
                }

                @Override
                public boolean isOther() {
                    return false;
                }

                @Override
                public long size() {
                    return 0;
                }

                @Override
                public Object fileKey() {
                    return null;
                }
            };
        } catch (IOException e) {
            throw new BuildContextException(e);
        }
    }

    @Override
    public Collection<? extends DefaultInputMetadata> registerInputs(
            Path basedir, Collection<String> includes, Collection<String> excludes) {
        basedir = normalize(basedir);
        Map<Path, FileMatcher> matchers = FileMatcher.createMatchers(basedir, includes, excludes);
        List<DefaultInputMetadata> result = matchers.entrySet().stream()
                .flatMap(e -> workspace
                        .walk(e.getKey())
                        .filter(s ->
                                !Files.isDirectory(s.getPath()) && e.getValue().matches(s.getPath())))
                .map(s -> {
                    if (s.getStatus() == Status.REMOVED) {
                        deletedResources.add(s.getPath());
                    } else {
                        registerInput(new FileState(s.getPath(), s.getLastModified(), s.getSize()));
                    }
                    return new DefaultInputMetadata(DefaultBuildContext.this, oldState, s.getPath());
                })
                .collect(Collectors.toList());
        if (workspace.getMode() == Workspace.Mode.DELTA) {
            // only NEW, MODIFIED and REMOVED resources are reported in DELTA mode
            // need to find any UNMODIFIED
            final FileMatcher absoluteMatcher = FileMatcher.createMatcher(basedir, includes, excludes);
            for (FileState fileState : oldState.getResources().values()) {
                Path path = fileState.getPath();
                if (!state.isResource(path) && !deletedResources.contains(path) && absoluteMatcher.matches(path)) {
                    result.add(registerNormalizedInput(path, fileState.getLastModified(), fileState.getSize()));
                }
            }
        }
        return result;
    }

    @Override
    public Collection<? extends DefaultInput> registerAndProcessInputs(
            Path basedir, Collection<String> includes, Collection<String> excludes) {
        return registerInputs(basedir, includes, excludes).stream()
                .map(m -> {
                    switch (m.getStatus()) {
                        case NEW:
                        case MODIFIED:
                            return processInput(m);
                        default:
                            return new DefaultInput(this, state, m.getPath());
                    }
                })
                .collect(Collectors.toList());
    }

    /**
     * Marks skipped build execution. All inputs, outputs and their associated metadata are carried
     * over to the next build as-is. No context modification operations (register* or process) are
     * permitted after this call.
     */
    @Override
    public void markSkipExecution() {
        if (!processedResources.isEmpty()) {
            throw new IllegalStateException();
        }
        closed = true;
    }

    protected DefaultInputMetadata registerNormalizedInput(Path resourceFile, FileTime lastModified, long length) {
        assertOpen();
        if (!state.isResource(resourceFile)) {
            registerInput(newFileState(resourceFile, lastModified, length));
        }
        return new DefaultInputMetadata(this, oldState, resourceFile);
    }

    private FileState newFileState(Path path) {
        BasicFileAttributes attrs = readAttributes(path);
        return newFileState(path, attrs.lastModifiedTime(), attrs.size());
    }

    private FileState newFileState(Path file, FileTime lastModified, long size) {
        if (!workspace.isPresent(file)) {
            throw new IllegalArgumentException("File does not exist or cannot be read " + file);
        }
        return new FileState(file, lastModified, size);
    }

    protected DefaultOutputMetadata registerNormalizedOutput(Path outputFile) {
        assertOpen();
        if (!state.isResource(outputFile)) {
            state.putResource(outputFile, null); // placeholder
            state.addOutput(outputFile);
        } else {
            if (!state.isOutput(outputFile)) {
                throw new IllegalStateException("Already registered as input " + outputFile);
            }
        }
        return new DefaultOutputMetadata(this, oldState, outputFile);
    }

    public boolean aggregate(
            Collection<? extends DefaultInputMetadata> inputs,
            Path outputFile,
            BiConsumer<Output, Collection<Input>> creator) {
        DefaultOutputMetadata output = registerOutput(outputFile);
        return aggregate(inputs, output, creator);
    }

    public boolean aggregate(
            Collection<? extends DefaultInputMetadata> inputs,
            DefaultOutputMetadata output,
            BiConsumer<Output, Collection<Input>> creator) {
        associate(inputs, output);
        boolean processingRequired = isEscalated();
        if (!processingRequired) {
            processingRequired = isProcessingRequired(inputs, output);
        }
        if (processingRequired) {
            DefaultOutput outputResource = processOutput(output);
            List<Input> inputResources = inputs.stream().map(this::processInput).collect(Collectors.toList());
            creator.accept(outputResource, inputResources);
        } else {
            markUptodateOutput(output.getPath());
        }
        return processingRequired;
    }

    public <T extends Serializable> boolean aggregate(
            Collection<? extends DefaultInputMetadata> inputs,
            Path outputFile,
            String stepId,
            T identity,
            Function<Input, T> mapper,
            BinaryOperator<T> accumulator,
            BiConsumer<Output, T> writer) {
        DefaultOutputMetadata output = registerOutput(outputFile);
        associate(inputs, output);
        boolean processingRequired = isEscalated() || isProcessingRequired(inputs, output);
        if (processingRequired) {
            T metadata = inputs.stream()
                    .map(input -> getMetadata(input, stepId, mapper))
                    .reduce(identity, accumulator);
            T oldMetadata = getOutputInputs(oldState, outputFile).stream()
                    .map(inputFile -> oldState.<T>getResourceAttribute(inputFile, stepId))
                    .reduce(identity, accumulator);
            if (!Objects.equals(metadata, oldMetadata)) {
                DefaultOutput outputResource = processOutput(output);
                writer.accept(outputResource, metadata);
                return true;
            }
        } else {
            markUptodateOutput(output.getPath());
        }
        return false;
    }

    private <T extends Serializable> T getMetadata(
            DefaultMetadata<Input> input, String stepId, Function<Input, T> mapper) {
        if (input.getStatus() != Status.UNMODIFIED) {
            return mapper.apply(input.process());
        } else {
            return oldState.getResourceAttribute(input.getPath(), stepId);
        }
    }

    protected void finalizeContext() {
        // only supports simple input --> output associations
        // outputs are carried over iff their input is carried over

        // TODO harden the implementation
        //
        // things can get tricky even with such simple model. consider the following
        // build-1: inputA --> outputA
        // build-2: inputA unchanged. inputB --> outputA
        // now outputA has multiple inputs, which is not supported by this context
        //
        // another tricky example
        // build-1: inputA --> outputA
        // build-2: inputA unchanged before the build, inputB --> inputA
        // now inputA is both input and output, which is not supported by this context

        // multi-pass implementation
        // pass 1, carry-over up-to-date inputs and collect all up-to-date outputs
        // pass 2, carry-over all up-to-date outputs
        // pass 3, remove obsolete and orphaned outputs

        Set<Path> uptodateOldOutputs = new HashSet<>();
        Set<Path> uptodateOldInputs = new HashSet<>();
        for (Path resource : oldState.getResources().keySet()) {
            if (oldState.isOutput(resource)) {
                continue;
            }

            if (isProcessedResource(resource) || isDeletedResource(resource) || !isRegisteredResource(resource)) {
                // deleted or processed resource, nothing to carry over
                continue;
            }

            if (state.isOutput(resource)) {
                // resource flipped from input to output without going through delete
                throw new BuildContextException(
                        new IllegalStateException("Inconsistent resource type change " + resource));
            }

            // carry over
            state.putResource(resource, oldState.getResource(resource));
            state.setResourceMessages(resource, oldState.getResourceMessages(resource));
            state.setResourceAttributes(resource, oldState.getResourceAttributes(resource));
            state.setResourceOutputs(resource, oldState.getResourceOutputs(resource));
            uptodateOldInputs.add(resource);
        }

        for (Path oldOutput : oldState.getOutputs()) {
            Collection<Path> outputInputs = oldState.getOutputInputs(oldOutput);
            if (outputInputs != null && uptodateOldInputs.containsAll(outputInputs)) {
                uptodateOldOutputs.add(oldOutput);
            }
        }

        for (Path output : uptodateOldOutputs) {
            if (state.isResource(output)) {
                // can't carry-over registered resources
                //                throw new IllegalStateException( "Can't carry over " + output );
            }

            state.putResource(output, oldState.getResource(output));
            state.addOutput(output);
            state.setResourceMessages(output, oldState.getResourceMessages(output));
            state.setResourceAttributes(output, oldState.getResourceAttributes(output));
        }

        for (Path output : oldState.getOutputs()) {
            if (!state.isOutput(output)) {
                deleteOutput(output);
            }
        }
    }

    protected void deleteOutput(Path resource) {
        if (!oldState.isOutput(resource) && !state.isOutput(resource)) {
            // not an output known to this build context
            throw new IllegalArgumentException();
        }

        workspace.deleteFile(resource);

        deletedResources.add(resource);
        processedResources.add(resource);

        state.removeResource(resource);
        state.removeOutput(resource);

        state.removeResourceAttributes(resource);
        state.removeResourceMessages(resource);
        state.removeResourceOutputs(resource);
    }

    protected boolean isEscalated() {
        return escalated;
    }

    // re-create output if any its inputs were added, changed or deleted since previous build
    private boolean isProcessingRequired(
            Collection<? extends DefaultInputMetadata> inputs, DefaultOutputMetadata output) {
        if (getResourceStatus(output.getPath()) == Status.MODIFIED) {
            return true;
        }
        if (inputs.stream().anyMatch(r -> r.getStatus() != Status.UNMODIFIED)) {
            return true;
        }
        List<Path> inputFiles = inputs.stream().map(Metadata::getPath).collect(Collectors.toList());
        return getOutputInputs(oldState, output.getPath()).stream().anyMatch(r -> !inputFiles.contains(r));
    }

    protected boolean isProcessedResource(Path resource) {
        return processedResources.contains(resource);
    }

    protected Set<Path> getProcessedResources() {
        return processedResources;
    }

    protected boolean isProcessed() {
        return !processedResources.isEmpty();
    }

    protected void markProcessedResource(Path resource) {
        processedResources.add(resource);
    }

    private DefaultOutputMetadata registerOutput(Path outputFile) {
        outputFile = normalize(outputFile);
        if (isRegisteredResource(outputFile)) {
            // only allow single registration of the same output. not sure why/if multiple will be needed
            throw new BuildContextException(new IllegalStateException("Output already registered " + outputFile));
        }
        return registerNormalizedOutput(outputFile);
    }

    private Collection<Path> getOutputInputs(DefaultBuildContextState state, Path outputFile) {
        Collection<Path> inputs = state.getOutputInputs(outputFile);
        return inputs != null && !inputs.isEmpty() ? inputs : Collections.emptyList();
    }

    protected boolean isRegisteredResource(Path resource) {
        return state.isResource(resource);
    }

    protected boolean isDeletedResource(Path resource) {
        return deletedResources.contains(resource);
    }

    protected void markUptodateOutput(Path outputFile) {
        if (!oldState.isOutput(outputFile)) {
            throw new IllegalArgumentException();
        }
        state.putResource(outputFile, oldState.getResource(outputFile));
        state.addOutput(outputFile);
    }

    /**
     * Adds the resource to this build's resource set. The resource must exist, i.e. it's status must
     * not be REMOVED.
     */
    protected Path registerInput(FileState holder) {
        Path resource = holder.getPath();
        FileState other = state.getResource(resource);
        if (other == null) {
            if (getResourceStatus(holder) == Status.REMOVED) {
                throw new BuildContextException(new IllegalArgumentException("Resource does not exist " + resource));
            }
            state.putResource(resource, holder);
        } else {
            if (state.isOutput(resource)) {
                throw new BuildContextException(new IllegalStateException("Already registered as output " + resource));
            }
            if (!holder.equals(other)) {
                throw new BuildContextException(
                        new IllegalArgumentException("Inconsistent resource state " + resource));
            }
            state.putResource(resource, holder);
        }
        return resource;
    }

    private Status getResourceStatus(FileState fileState) {
        return workspace.getResourceStatus(fileState.getPath(), fileState.getLastModified(), fileState.getSize());
    }

    private void assertOpen() {
        if (closed) {
            throw new IllegalStateException();
        }
    }

    protected DefaultInput processInput(DefaultInputMetadata metadata) {
        final Path resource = metadata.getPath();
        if (metadata.context != this || !state.isResource(resource)) {
            throw new IllegalArgumentException();
        }
        processResource(resource);
        return new DefaultInput(this, state, resource);
    }

    private void processResource(final Path resource) {
        processedResources.add(resource);

        // reset all metadata associated with the resource during this build
        //            state.removeResourceAttributes( resource );
        //            state.removeResourceMessages( resource );
        //            state.removeResourceOutputs( resource );
    }

    protected Status getResourceStatus(Path resource) {
        if (deletedResources.contains(resource)) {
            return Status.REMOVED;
        }

        FileState oldResourceState = oldState.getResource(resource);
        if (oldResourceState == null) {
            return Status.NEW;
        }

        Status status = getResourceStatus(oldResourceState);

        if (status == Status.UNMODIFIED && escalated) {
            status = Status.MODIFIED;
        }

        return status;
    }

    protected DefaultOutput associate(DefaultInput input, DefaultOutput output) {
        if (input.context != this) {
            throw new BuildContextException(new IllegalArgumentException());
        }
        if (output.context != this) {
            throw new BuildContextException(new IllegalArgumentException());
        }

        assertAssociation(input, output);

        state.putResourceOutput(input.getPath(), output.getPath());
        return output;
    }

    private void associate(Iterable<? extends DefaultInputMetadata> inputs, DefaultOutputMetadata output) {
        inputs.forEach(r -> state.putResourceOutput(r.getPath(), output.getPath()));
    }

    protected Collection<? extends DefaultOutputMetadata> getAssociatedOutputs(
            DefaultBuildContextState state, Path resource) {
        Collection<Path> outputFiles = state.getResourceOutputs(resource);
        if (outputFiles == null || outputFiles.isEmpty()) {
            return Collections.emptyList();
        }
        List<DefaultOutputMetadata> outputs = new ArrayList<>();
        for (Path outputFile : outputFiles) {
            outputs.add(new DefaultOutputMetadata(this, state, outputFile));
        }
        return outputs;
    }

    protected void assertAssociation(DefaultInput resource, DefaultOutput output) {
        Path input = resource.getPath();
        Path outputFile = output.getPath();

        // input --> output --> output2 is not supported (until somebody provides a usecase)
        if (state.isOutput(input)) {
            throw new BuildContextException(new UnsupportedOperationException());
        }

        // each output can only be associated with a single input
        Collection<Path> inputs = state.getOutputInputs(outputFile);
        if (inputs != null && !inputs.isEmpty() && !containsOnly(inputs, input)) {
            throw new BuildContextException(new UnsupportedOperationException());
        }
    }

    protected <T extends Serializable> Serializable setResourceAttribute(Path resource, String key, T value) {
        state.putResourceAttribute(resource, key, value);
        // TODO odd this always returns previous build state. need to think about it
        return oldState.getResourceAttribute(resource, key);
    }

    protected <T extends Serializable> T getResourceAttribute(
            DefaultBuildContextState state, Path resource, String key, Class<T> clazz) {
        Map<String, Serializable> attributes = state.getResourceAttributes(resource);
        return attributes != null ? clazz.cast(attributes.get(key)) : null;
    }

    void addMessage(Path resource, int line, int column, String message, Severity severity, Throwable cause) {
        // this is likely called as part of builder error handling logic.
        // to make IAE easier to troubleshoot, link cause to the exception thrown
        if (resource == null) {
            throw new IllegalArgumentException("resource cannot be null", cause);
        }
        if (severity == null) {
            throw new IllegalArgumentException("severity cannot be null", cause);
        }
        state.addResourceMessage(resource, new Message(line, column, message, severity, cause));
        log(resource, line, column, message, severity, cause);
    }

    OutputStream newOutputStream(DefaultOutput output) {
        return workspace.newOutputStream(output.getPath());
    }

    DefaultOutput newOutput(DefaultOutputMetadata resource) {
        return new DefaultOutput(this, state, resource.resource);
    }

    public void commit(@Nullable Sink sink) {
        if (closed) {
            return;
        }
        this.closed = true;

        // messages recorded during this build
        Map<Path, Collection<Message>> newMessages = new HashMap<>(state.getResourceMessages());

        finalizeContext();

        // assert inputs didn't change
        for (Map.Entry<Path, FileState> entry : state.getResources().entrySet()) {
            Path resource = entry.getKey();
            FileState holder = entry.getValue();
            if (!state.isOutput(resource) && holder.getStatus() != Status.UNMODIFIED) {
                throw new BuildContextException(new IllegalStateException("Unexpected input change " + resource));
            }
        }

        // timestamp new outputs
        state.getOutputs().forEach(outputFile -> state.computeResourceIfAbsent(outputFile, this::newFileState));

        if (stateFile != null) {
            try (OutputStream os = workspace.newOutputStream(stateFile)) {
                state.storeTo(os);
            } catch (IOException e) {
                throw new BuildContextException(e);
            }
        }

        // new messages are logged as soon as they are reported during the build
        // replay old messages so the user can still see them
        Map<Path, Collection<Message>> allMessages = new HashMap<>(state.getResourceMessages());

        if (!allMessages.keySet().equals(newMessages.keySet())) {
            for (Map.Entry<Path, Collection<Message>> entry : allMessages.entrySet()) {
                Path resource = entry.getKey();
                if (!newMessages.containsKey(resource)) {
                    for (Message message : entry.getValue()) {
                        log(
                                resource,
                                message.getLine(),
                                message.getColumn(),
                                message.getMessage(),
                                message.getSeverity(),
                                message.getCause());
                    }
                }
            }
        }

        // processedResources includes resources added, changed and deleted during this build
        // clear all old messages associated with the processed resources during previous builds
        if (sink != null) {
            for (Path resource : processedResources) {
                sink.clear(resource);
            }
            for (Path resource : oldState.getResources().keySet()) {
                if (!state.isResource(resource)) {
                    sink.clear(resource);
                }
            }
            for (Map.Entry<Path, Collection<Message>> entry : allMessages.entrySet()) {
                Path resource = entry.getKey();
                boolean isNew = newMessages.containsKey(resource);
                sink.messages(resource, isNew, entry.getValue());
            }
        }
    }

    private void log(Path resource, int line, int column, String message, Severity severity, Throwable cause) {
        // TODO
    }
}
