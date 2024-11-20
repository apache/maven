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
package org.apache.maven.cling.invoker.mvn.forked;

import java.io.IOException;
import java.util.ArrayList;

import org.apache.maven.api.cli.Executor;
import org.apache.maven.api.cli.ExecutorException;
import org.apache.maven.api.cli.ExecutorRequest;
import org.apache.maven.internal.impl.model.profile.Os;

import static java.util.Objects.requireNonNull;

/**
 * Forked executor implementation, that spawns a subprocess with Maven from the installation directory.
 */
public class ForkedMavenExecutor implements Executor {
    @Override
    public int execute(ExecutorRequest executorRequest) throws ExecutorException {
        requireNonNull(executorRequest);
        validate(executorRequest);

        ArrayList<String> cmdAndArguments = new ArrayList<>();
        cmdAndArguments.add(executorRequest
                .installationDirectory()
                .resolve("bin")
                .resolve(
                        Os.IS_WINDOWS
                                ? executorRequest.parserRequest().command() + ".cmd"
                                : executorRequest.parserRequest().command())
                .toString());

        cmdAndArguments.addAll(executorRequest.parserRequest().args());

        try {
            ProcessBuilder pb = new ProcessBuilder()
                    .directory(executorRequest.cwd().toFile())
                    .command(cmdAndArguments);

            if (executorRequest.jvmArguments().isPresent()) {
                pb.environment()
                        .put(
                                "MAVEN_OPTS",
                                String.join(" ", executorRequest.jvmArguments().get()));
            }

            return pb.start().waitFor();
        } catch (IOException e) {
            throw new ExecutorException("IO problem while executing command: " + cmdAndArguments, e);
        } catch (InterruptedException e) {
            throw new ExecutorException("Interrupted while executing command: " + cmdAndArguments, e);
        }
    }

    protected void validate(ExecutorRequest executorRequest) throws ExecutorException {}
}
