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
package org.apache.maven.internal.build.impl.maven;

import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Provider;

import java.nio.file.Path;
import java.util.Collection;

import org.apache.maven.api.build.InputSet;
import org.apache.maven.api.build.spi.BuildContextEnvironment;
import org.apache.maven.api.build.spi.CommitableBuildContext;
import org.apache.maven.api.build.spi.Sink;
import org.apache.maven.execution.scope.MojoExecutionScoped;
import org.apache.maven.internal.build.impl.DefaultBuildContext;
import org.apache.maven.internal.build.impl.DefaultInput;
import org.apache.maven.internal.build.impl.DefaultInputMetadata;
import org.apache.maven.internal.build.impl.DefaultOutput;
import org.eclipse.sisu.Typed;

@Named
public class MavenBuildContext implements CommitableBuildContext {

    private final Provider<MojoExecutionScopedBuildContext> provider;

    @Inject
    public MavenBuildContext(Provider<MojoExecutionScopedBuildContext> delegate) {
        this.provider = delegate;
    }

    MojoExecutionScopedBuildContext getDelegate() {
        return provider.get();
    }

    public boolean getFailOnError() {
        return getDelegate().getFailOnError();
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
    public void commit(Sink sink) {
        getDelegate().commit(sink);
    }

    @Named
    @Typed(MojoExecutionScopedBuildContext.class)
    @MojoExecutionScoped
    public static class MojoExecutionScopedBuildContext extends DefaultBuildContext {
        @Inject
        public MojoExecutionScopedBuildContext(BuildContextEnvironment configuration) {
            super(configuration);
        }
    }
}
