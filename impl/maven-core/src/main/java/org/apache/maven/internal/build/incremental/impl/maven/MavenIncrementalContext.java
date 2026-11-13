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
package org.apache.maven.internal.build.incremental.impl.maven;

import java.nio.file.Path;
import java.util.Collection;
import java.util.function.Supplier;

import org.apache.maven.api.build.incremental.InputSet;
import org.apache.maven.api.build.incremental.spi.CommittableIncrementalContext;
import org.apache.maven.api.build.incremental.spi.IncrementalContextEnvironment;
import org.apache.maven.api.di.Inject;
import org.apache.maven.api.di.MojoExecutionScoped;
import org.apache.maven.api.di.Named;
import org.apache.maven.api.di.Typed;
import org.apache.maven.api.services.PathMatcherFactory;
import org.apache.maven.internal.build.incremental.impl.DefaultIncrementalContext;
import org.apache.maven.internal.build.incremental.impl.DefaultInput;
import org.apache.maven.internal.build.incremental.impl.DefaultInputMetadata;
import org.apache.maven.internal.build.incremental.impl.DefaultOutput;

/**
 * The Maven runtime's {@link org.apache.maven.api.build.incremental.IncrementalContext IncrementalContext}
 * implementation, delegating to a {@link MojoExecutionScopedIncrementalContext} that is created
 * fresh for each mojo execution.
 *
 * <p>This class is injected into mojos as the {@code IncrementalContext} binding. It uses a
 * {@link Supplier} to lazily obtain the scoped delegate, ensuring that each mojo execution
 * gets its own isolated build context with its own state file, configuration digest, and
 * input/output tracking.</p>
 *
 * @since 4.1.0
 * @see MojoExecutionScopedIncrementalContext
 * @see MavenIncrementalContextConfiguration
 */
@Named
public class MavenIncrementalContext implements CommittableIncrementalContext {

    private final Supplier<MojoExecutionScopedIncrementalContext> provider;

    @Inject
    public MavenIncrementalContext(Supplier<MojoExecutionScopedIncrementalContext> delegate) {
        this.provider = delegate;
    }

    MojoExecutionScopedIncrementalContext getDelegate() {
        return provider.get();
    }

    public boolean isFailOnError() {
        return getDelegate().isFailOnError();
    }

    @Override
    public boolean isProcessingRequired() {
        return getDelegate().isProcessingRequired();
    }

    @Override
    public DefaultOutput processOutput(Path outputFile) {
        return getDelegate().processOutput(outputFile);
    }

    @Override
    public InputSet newInputSet() {
        return getDelegate().newInputSet();
    }

    @Override
    public DefaultInputMetadata registerInput(Path inputFile) {
        return getDelegate().registerInput(inputFile);
    }

    @Override
    public Collection<? extends DefaultInputMetadata> registerInputs(
            Path basedir, Collection<String> includes, Collection<String> excludes) {
        return getDelegate().registerInputs(basedir, includes, excludes);
    }

    @Override
    public Collection<? extends DefaultInput> registerAndProcessInputs(
            Path basedir, Collection<String> includes, Collection<String> excludes) {
        return getDelegate().registerAndProcessInputs(basedir, includes, excludes);
    }

    @Override
    public void markSkipExecution() {
        getDelegate().markSkipExecution();
    }

    @Override
    public void setFailOnError(boolean failOnError) {
        getDelegate().setFailOnError(failOnError);
    }

    @Override
    public void commit() {
        getDelegate().commit();
    }

    /**
     * The per-mojo-execution build context instance. Created once per mojo execution via
     * the {@link MojoExecutionScoped} DI scope, initialized from a
     * {@link MavenIncrementalContextConfiguration} that provides the state file location,
     * configuration digest, workspace, and finalizer.
     *
     * <p>At construction time, the context loads the previous build state (if any),
     * compares the configuration digest, and determines whether to escalate to a full
     * build. All subsequent {@code registerInputs} / {@code associateOutput} calls
     * operate on this instance's state, which is committed at the end of mojo execution
     * by the {@link MavenIncrementalContextFinalizer}.</p>
     */
    @Named
    @Typed(MojoExecutionScopedIncrementalContext.class)
    @MojoExecutionScoped
    public static class MojoExecutionScopedIncrementalContext extends DefaultIncrementalContext {
        @Inject
        public MojoExecutionScopedIncrementalContext(
                IncrementalContextEnvironment configuration, PathMatcherFactory pathMatcherFactory) {
            super(configuration, pathMatcherFactory);
        }
    }
}
