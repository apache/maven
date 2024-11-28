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
package org.apache.maven.cling.executor;

import org.apache.maven.api.annotations.Nonnull;
import org.apache.maven.api.cli.Executor;
import org.apache.maven.api.cli.ExecutorException;
import org.apache.maven.api.cli.ExecutorRequest;

/**
 * Helper class for some common tasks.
 */
public interface ExecutorHelper extends ExecutorTool, AutoCloseable {
    /**
     * The preferred mode of execution.
     */
    enum Mode {
        /**
         * Automatically decide. For example, presence of {@link ExecutorRequest#environmentVariables()} or
         * {@link ExecutorRequest#jvmArguments()} will result in choosing forked executor. Otherwise, embedded
         * executor is preferred.
         */
        AUTO,
        /**
         * Forces embedded execution. May fail if {@link ExecutorRequest} contains input unsupported by executor.
         */
        EMBEDDED,
        /**
         * Forces forked execution. Always carried out, but slow as it uses child process.
         */
        FORKED
    }

    /**
     * Creates pre-populated builder for {@link ExecutorRequest}.
     */
    @Nonnull
    ExecutorRequest.Builder executorRequest();

    /**
     * Executes the request with automatically chosen executor.
     */
    default int execute(ExecutorRequest executorRequest) throws ExecutorException {
        return execute(Mode.AUTO, executorRequest);
    }

    /**
     * Executes the request with chosen executor by passed in mode.
     */
    int execute(Mode mode, ExecutorRequest executorRequest) throws ExecutorException;

    /**
     * High level operation.
     * Returns the version of the Maven covered by this helper.
     *
     * @see Executor#mavenVersion(ExecutorRequest)
     */
    @Nonnull
    String mavenVersion();

    /**
     * Closes helper, frees resources.
     */
    @Override
    void close() throws ExecutorException;
}
