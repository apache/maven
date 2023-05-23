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

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.apache.maven.api.build.BuildContext;
import org.apache.maven.api.build.Severity;
import org.apache.maven.api.build.spi.BuildContextFinalizer;
import org.apache.maven.api.build.spi.CommitableBuildContext;
import org.apache.maven.api.build.spi.Message;
import org.apache.maven.api.build.spi.Sink;
import org.apache.maven.execution.MojoExecutionEvent;
import org.apache.maven.execution.scope.MojoExecutionScoped;
import org.apache.maven.execution.scope.WeakMojoExecutionListener;
import org.apache.maven.plugin.MojoExecutionException;
import org.eclipse.sisu.Nullable;

@Named
@MojoExecutionScoped
public class MavenBuildContextFinalizer implements WeakMojoExecutionListener, BuildContextFinalizer {

    private final List<CommitableBuildContext> contexts = new ArrayList<>();

    private final Sink sink;

    @Inject
    public MavenBuildContextFinalizer(@Nullable Sink sink) {
        this.sink = sink;
    }

    public void registerContext(CommitableBuildContext context) {
        contexts.add(context);
    }

    protected List<? extends BuildContext> getRegisteredContexts() {
        return contexts;
    }

    @Override
    public void afterMojoExecutionSuccess(MojoExecutionEvent event) throws MojoExecutionException {
        try {
            final Map<Path, Collection<Message>> allMessages = new HashMap<>();
            for (CommitableBuildContext context : contexts) {
                context.commit(new Sink() {
                    @Override
                    public void clear(Path resource) {
                        if (sink != null) {
                            sink.clear(resource);
                        }
                    }

                    @Override
                    public void messages(Path resource, boolean isNew, Collection<Message> messages) {
                        if (sink != null) {
                            sink.messages(resource, isNew, messages);
                        }
                        allMessages.put(resource, messages);
                    }
                });
            }

            if (sink == null) {
                failBuild(allMessages);
            }
        } catch (Exception e) {
            throw new MojoExecutionException("Could not maintain incremental build state", e);
        }
    }

    protected void failBuild(final Map<Path, Collection<Message>> messages) throws MojoExecutionException {
        // without messageSink, have to raise exception if there were errors
        int errorCount = 0;
        StringBuilder errors = new StringBuilder();
        for (Map.Entry<Path, Collection<Message>> entry : messages.entrySet()) {
            Object resource = entry.getKey();
            for (Message message : entry.getValue()) {
                if (message.getSeverity() == Severity.ERROR) {
                    errorCount++;
                    errors.append(String.format(
                            "%s:[%d:%d] %s\n",
                            resource.toString(), message.getLine(), message.getColumn(), message.getMessage()));
                }
            }
        }
        final Set<Boolean> failOnErrors = extractFailOnErrors(contexts);
        if (failOnErrors.size() != 1) {
            throw new IllegalStateException("Contexts FailOnError property have different values.");
        }

        final Boolean failOnError = failOnErrors.iterator().next();
        if (errorCount > 0 && failOnError) {
            throw new MojoExecutionException(errorCount + " error(s) encountered:\n" + errors);
        }
    }

    private Set<Boolean> extractFailOnErrors(List<CommitableBuildContext> contexts) {
        final Set<Boolean> result = new HashSet<>();
        for (BuildContext context : contexts) {
            result.add(context.getFailOnError());
        }
        return result;
    }

    @Override
    public void beforeMojoExecution(MojoExecutionEvent event) throws MojoExecutionException {}

    @Override
    public void afterExecutionFailure(MojoExecutionEvent event) {}
}
