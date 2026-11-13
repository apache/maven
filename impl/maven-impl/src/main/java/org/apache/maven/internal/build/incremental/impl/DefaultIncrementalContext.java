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

import java.io.IOException;
import java.io.OutputStream;
import java.io.Serializable;
import java.nio.file.Files;
import java.nio.file.NoSuchFileException;
import java.nio.file.Path;
import java.nio.file.PathMatcher;
import java.nio.file.attribute.BasicFileAttributes;
import java.nio.file.attribute.FileTime;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
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
import org.apache.maven.api.build.incremental.IncrementalContextException;
import org.apache.maven.api.build.incremental.Input;
import org.apache.maven.api.build.incremental.Metadata;
import org.apache.maven.api.build.incremental.Output;
import org.apache.maven.api.build.incremental.Status;
import org.apache.maven.api.build.incremental.spi.CommittableIncrementalContext;
import org.apache.maven.api.build.incremental.spi.IncrementalContextEnvironment;
import org.apache.maven.api.build.incremental.spi.IncrementalContextFinalizer;
import org.apache.maven.api.build.incremental.spi.Workspace;
import org.apache.maven.api.services.PathMatcherFactory;
import org.apache.maven.impl.DefaultPathMatcherFactory;

/**
 * Default implementation of the incremental build context.
 *
 * <p>This class manages the full lifecycle of an incremental build: loading previous state,
 * comparing configuration and file timestamps, tracking input-output associations, and
 * cleaning up stale outputs at commit time.</p>
 *
 * <h2>Escalation</h2>
 *
 * <p>At construction, the context decides whether to <em>escalate</em> to a full build
 * (treating all inputs as modified). Escalation is triggered when:</p>
 * <ul>
 *   <li>The previous state file does not exist or cannot be read (first build or after clean)</li>
 *   <li>The {@linkplain #getConfigurationChanged() configuration has changed} — any entry
 *       in the configuration map differs from the previous build's stored configuration</li>
 *   <li>Any previously tracked output file is missing on disk</li>
 *   <li>The workspace explicitly requests escalation
 *       ({@link Workspace.Mode#ESCALATED})</li>
 * </ul>
 *
 * <h2>Stale output cleanup</h2>
 *
 * <p>At {@linkplain #finalizeContext() commit time}, the context performs a three-pass cleanup:</p>
 * <ol>
 *   <li>Carry over unprocessed (up-to-date) inputs and collect their associated outputs</li>
 *   <li>Carry over outputs whose inputs are all up-to-date</li>
 *   <li>Delete outputs from the previous build that are not carried over — these are
 *       <em>stale outputs</em> whose inputs were removed, modified, or re-associated</li>
 * </ol>
 *
 * @since 4.1.0
 */
public class DefaultIncrementalContext implements CommittableIncrementalContext {
    final Workspace workspace;
    final Path stateFile;
    final DefaultIncrementalContextState state;
    final DefaultIncrementalContextState oldState;
    /**
     * Whether the build has been escalated to a full rebuild. When escalated, all input
     * files are treated as requiring processing regardless of their actual file timestamps.
     * Escalation is triggered by configuration changes, missing state, or missing outputs.
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
     * Resources registered as inputs during this build via {@link #registerInput}.
     * Used to scope the concurrent-modification assertion in {@link #commit} — only resources
     * registered during this build are checked; resources carried over from a previous build's
     * state (e.g. dependency JARs from other reactor modules) are not, since they may legitimately
     * change between registration and commit in a reactor build.
     */
    private final Set<Path> registeredResources = new HashSet<>();
    /**
     * Indicates that no further modifications to this build context are allowed.
     */
    private boolean closed;
    /**
     * Indicates whether the build will continue even if there are compilation errors.
     */
    private boolean failOnError = true;

    /**
     * Factory for creating path matchers with Ant-style pattern support.
     */
    private final PathMatcherFactory pathMatcherFactory;

    public DefaultIncrementalContext(IncrementalContextEnvironment env) {
        this(env.getWorkspace(), env.getStateFile(), env.getParameters(), env.getFinalizer(), null);
    }

    public DefaultIncrementalContext(
            IncrementalContextEnvironment env, @Nullable PathMatcherFactory pathMatcherFactory) {
        this(env.getWorkspace(), env.getStateFile(), env.getParameters(), env.getFinalizer(), pathMatcherFactory);
    }

    public DefaultIncrementalContext(
            Workspace workspace,
            Path stateFile,
            Map<String, Serializable> configuration,
            IncrementalContextFinalizer finalizer) {
        this(workspace, stateFile, configuration, finalizer, null);
    }

    /**
     * Creates a new build context with the given workspace, state file, and configuration.
     *
     * <p>The constructor loads the previous build state from {@code stateFile} (if it exists),
     * then compares each entry in {@code configuration} against the stored values. If any
     * configuration value has changed — or if the state file is missing or any previously
     * tracked output has been deleted — the context escalates to a full build.</p>
     *
     * <p>The {@code configuration} map typically contains digested mojo parameters and
     * plugin classpath hashes, provided by the Maven runtime. See
     * {@link org.apache.maven.api.build.incremental.spi.IncrementalContextEnvironment#getParameters()
     * IncrementalContextEnvironment.getParameters()} for details.</p>
     *
     * @param workspace           the file-system abstraction for reading, writing, and deleting files
     * @param stateFile           the path to the binary state file, or {@code null} for a
     *                            stateless (always-escalated) context
     * @param configuration       the configuration parameters to compare against the previous build
     * @param finalizer           optional callback that commits the context after mojo execution
     * @param pathMatcherFactory  optional factory for Ant-style path matchers; defaults to
     *                            {@link DefaultPathMatcherFactory} if {@code null}
     */
    public DefaultIncrementalContext(
            Workspace workspace,
            Path stateFile,
            Map<String, Serializable> configuration,
            IncrementalContextFinalizer finalizer,
            @Nullable PathMatcherFactory pathMatcherFactory) {
        // preconditions
        Objects.requireNonNull(workspace, "workspace");
        Objects.requireNonNull(configuration, "configuration");

        this.pathMatcherFactory = pathMatcherFactory != null ? pathMatcherFactory : new DefaultPathMatcherFactory();
        this.stateFile = stateFile != null ? stateFile.toAbsolutePath() : null;
        this.state = DefaultIncrementalContextState.withConfiguration(configuration);
        this.oldState = DefaultIncrementalContextState.loadFrom(this.stateFile);

        final boolean configurationChanged = getConfigurationChanged();
        if (workspace.getMode() == Workspace.Mode.ESCALATED) {
            this.escalated = true;
            this.workspace = workspace;
        } else if (workspace.getMode() == Workspace.Mode.SUPPRESSED) {
            this.escalated = false;
            this.workspace = workspace;
        } else if (configurationChanged || !oldState.getOutputs().isEmpty() && !isPresent(oldState.getOutputs())) {
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
        return getCanonicalPath(input);
    }

    static Path getCanonicalPath(Path path) {
        try {
            return path.toRealPath();
        } catch (IOException e) {
            Path parent = path.getParent();
            if (parent == null) {
                return path.toAbsolutePath().normalize();
            }
            return getCanonicalPath(parent).resolve(path.getFileName());
        }
    }

    public boolean isFailOnError() {
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

    /**
     * Determines whether the build configuration has changed since the previous build.
     *
     * <p>Compares every key in the union of the current and previous configuration maps.
     * A change in any value (including keys present in one map but not the other) triggers
     * escalation to a full build. Values are compared using {@link Objects#equals}, so
     * the digest values must implement {@code equals()} correctly (e.g., byte-array wrappers
     * like {@code BytesHash}).</p>
     *
     * @return {@code true} if any configuration entry differs from the previous build
     */
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
            throw new IncrementalContextException(e);
        }
    }

    @Override
    public Collection<? extends DefaultInputMetadata> registerInputs(
            Path basedir, Collection<String> includes, Collection<String> excludes) {
        basedir = normalize(basedir);
        Map<Path, PathMatcher> matchers = pathMatcherFactory.createSubdirectoryMatchers(basedir, includes, excludes);
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
                    return new DefaultInputMetadata(DefaultIncrementalContext.this, oldState, s.getPath());
                })
                .collect(Collectors.toList());
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

    /**
     * Finalizes the build context by carrying over up-to-date state and cleaning up stale outputs.
     *
     * <p>This method implements a three-pass algorithm:</p>
     * <ol>
     *   <li><strong>Pass 1 — carry over up-to-date inputs.</strong> For each input from the
     *       previous build that was neither processed nor deleted in this build (and is still
     *       registered), carry over its state (timestamps, messages, attributes, output
     *       associations). Collect the set of outputs associated with these carried-over inputs.</li>
     *   <li><strong>Pass 2 — carry over up-to-date outputs.</strong> For each output from the
     *       previous build whose <em>all</em> associated inputs were carried over, carry over
     *       the output's state as well.</li>
     *   <li><strong>Pass 3 — delete stale outputs.</strong> Any output from the previous build
     *       that was <em>not</em> carried over (because its input was deleted, modified, or
     *       re-associated to a different output) is deleted from disk via the workspace.</li>
     * </ol>
     *
     * <p>This is the mechanism that provides automatic stale-output cleanup: when a source file
     * is removed, all output files that were associated with it via
     * {@link org.apache.maven.api.build.incremental.Input#associateOutput(Path)} in the previous
     * build are automatically deleted. For example, deleting {@code Foo.java} will clean up
     * {@code Foo.class}, {@code Foo$Inner.class}, and any other outputs associated with it.</p>
     *
     * <p><strong>Limitation:</strong> only simple input → output associations are supported.
     * An output with multiple inputs, or a resource that is both input and output, may produce
     * unexpected results.</p>
     */
    protected void finalizeContext() {

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
                throw new IncrementalContextException(
                        new IllegalStateException("Inconsistent resource type change " + resource));
            }

            // carry over metadata (messages, attributes, output associations) from previous build
            if (!registeredResources.contains(resource)) {
                // Resource was not re-registered during this build — carry over FileState too.
                // This preserves the previous build's mtime/size for unmodified resources.
                state.putResource(resource, oldState.getResource(resource));
            }
            // else: Resource was re-registered via registerInput() during this build — keep
            // the fresh FileState (with current filesystem mtime/size) rather than overwriting
            // with the old state. This is important in reactor builds where dependency JARs
            // from earlier modules are legitimately rebuilt with new timestamps.
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
            throw new IncrementalContextException(new IllegalStateException("Output already registered " + outputFile));
        }
        return registerNormalizedOutput(outputFile);
    }

    private Collection<Path> getOutputInputs(DefaultIncrementalContextState contextState, Path outputFile) {
        Collection<Path> inputs = contextState.getOutputInputs(outputFile);
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
     *
     * @param holder the file state of the resource to register
     * @return the normalized path of the registered resource
     */
    protected Path registerInput(FileState holder) {
        Path resource = holder.getPath();
        FileState other = state.getResource(resource);
        if (other == null) {
            if (getResourceStatus(holder) == Status.REMOVED) {
                throw new IncrementalContextException(
                        new IllegalArgumentException("Resource does not exist " + resource));
            }
            state.putResource(resource, holder);
        } else {
            if (state.isOutput(resource)) {
                throw new IncrementalContextException(
                        new IllegalStateException("Already registered as output " + resource));
            }
            if (!holder.equals(other)) {
                throw new IncrementalContextException(
                        new IllegalArgumentException("Inconsistent resource state " + resource));
            }
            state.putResource(resource, holder);
        }
        registeredResources.add(resource);
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
            throw new IncrementalContextException(new IllegalArgumentException());
        }
        if (output.context != this) {
            throw new IncrementalContextException(new IllegalArgumentException());
        }

        assertAssociation(input, output);

        state.putResourceOutput(input.getPath(), output.getPath());
        return output;
    }

    private void associate(Iterable<? extends DefaultInputMetadata> inputs, DefaultOutputMetadata output) {
        inputs.forEach(r -> state.putResourceOutput(r.getPath(), output.getPath()));
    }

    protected Collection<? extends DefaultOutputMetadata> getAssociatedOutputs(
            DefaultIncrementalContextState contextState, Path resource) {
        Collection<Path> outputFiles = contextState.getResourceOutputs(resource);
        if (outputFiles == null || outputFiles.isEmpty()) {
            return Collections.emptyList();
        }
        List<DefaultOutputMetadata> outputs = new ArrayList<>();
        for (Path outputFile : outputFiles) {
            outputs.add(new DefaultOutputMetadata(this, contextState, outputFile));
        }
        return outputs;
    }

    protected void assertAssociation(DefaultInput resource, DefaultOutput output) {
        Path input = resource.getPath();
        Path outputFile = output.getPath();

        // input --> output --> output2 is not supported (until somebody provides a usecase)
        if (state.isOutput(input)) {
            throw new IncrementalContextException(new UnsupportedOperationException());
        }

        // each output can only be associated with a single input
        Collection<Path> inputs = state.getOutputInputs(outputFile);
        if (inputs != null && !inputs.isEmpty() && !containsOnly(inputs, input)) {
            throw new IncrementalContextException(new UnsupportedOperationException());
        }
    }

    protected <T extends Serializable> Serializable setResourceAttribute(Path resource, String key, T value) {
        state.putResourceAttribute(resource, key, value);
        // TODO odd this always returns previous build state. need to think about it
        return oldState.getResourceAttribute(resource, key);
    }

    protected <T extends Serializable> T getResourceAttribute(
            DefaultIncrementalContextState contextState, Path resource, String key, Class<T> clazz) {
        Map<String, Serializable> attributes = contextState.getResourceAttributes(resource);
        return attributes != null ? clazz.cast(attributes.get(key)) : null;
    }

    OutputStream newOutputStream(DefaultOutput output) {
        return workspace.newOutputStream(output.getPath());
    }

    DefaultOutput newOutput(DefaultOutputMetadata resource) {
        return new DefaultOutput(this, state, resource.resource);
    }

    public void commit() {
        if (closed) {
            return;
        }
        this.closed = true;

        finalizeContext();

        // Assert that registered inputs were not concurrently modified during this build.
        // Only check resources registered during THIS build — resources carried over from a
        // previous build's state (e.g. dependency JARs from other reactor modules) are not
        // checked, since they may legitimately change between builds.
        for (Path resource : registeredResources) {
            FileState holder = state.getResource(resource);
            if (holder != null && !state.isOutput(resource) && holder.getStatus() != Status.UNMODIFIED) {
                throw new IncrementalContextException(new IllegalStateException("Unexpected input change " + resource));
            }
        }

        // timestamp new outputs
        state.getOutputs().forEach(outputFile -> state.computeResourceIfAbsent(outputFile, this::newFileState));

        // Skip state file write on no-op builds — if nothing was processed or deleted,
        // the state is identical to what was loaded. This saves significant I/O on the
        // most common incremental build scenario.
        boolean stateChanged = !processedResources.isEmpty() || !deletedResources.isEmpty() || escalated;
        if (stateFile != null && stateChanged) {
            try (OutputStream os = workspace.newOutputStream(stateFile)) {
                state.storeTo(os);
            } catch (IOException e) {
                throw new IncrementalContextException(e);
            }
        }
    }
}
