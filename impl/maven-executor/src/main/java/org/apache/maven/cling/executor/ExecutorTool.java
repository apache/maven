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

import java.util.Map;

import org.apache.maven.api.annotations.Nullable;
import org.apache.maven.api.cli.ExecutorException;
import org.apache.maven.api.cli.ExecutorRequest;

/**
 * A tool implementing some common Maven operations.
 */
public interface ExecutorTool {
    /**
     * Performs a diagnostic dump of the environment.
     *
     * @param request never {@code null}
     */
    Map<String, String> dump(ExecutorRequest.Builder request) throws ExecutorException;

    /**
     * Returns the location of local repository, as detected by Maven. The {@code userSettings} param may contain
     * some override (equivalent of {@code -s settings.xml} on CLI).
     *
     * @param request never {@code null}
     */
    String localRepository(ExecutorRequest.Builder request) throws ExecutorException;

    /**
     * Returns relative (to {@link #localRepository(ExecutorRequest.Builder)}) path of given artifact in local repository.
     *
     * @param request never {@code null}
     * @param gav the usual resolver artifact GAV string, never {@code null}
     * @param repositoryId the remote repository ID in case "remote artifact" is asked for
     */
    String artifactPath(ExecutorRequest.Builder request, String gav, @Nullable String repositoryId)
            throws ExecutorException;

    /**
     * Returns relative (to {@link #localRepository(ExecutorRequest.Builder)}) path of given metadata in local repository.
     * The metadata coordinates in form of {@code [G]:[A]:[V]:[type]}. Absence of {@code A} implies absence of {@code V}
     * as well (in other words, it can be {@code G}, {@code G:A} or {@code G:A:V}). The absence of {@code type} implies
     * it is "maven-metadata.xml". The simplest spec string is {@code :::}.
     * <p>
     * Examples:
     * <ul>
     *     <li>{@code :::} is root metadata named "maven-metadata.xml"</li>
     *     <li>{@code :::my-metadata.xml} is root metadata named "my-metadata.xml"</li>
     *     <li>{@code G:::} equals to {@code G:::maven-metadata.xml}</li>
     *     <li>{@code G:A::} equals to {@code G:A::maven-metadata.xml}</li>
     * </ul>
     *
     * @param request never {@code null}
     * @param gav the resolver metadata GAV string
     * @param repositoryId the remote repository ID in case "remote metadata" is asked for
     */
    String metadataPath(ExecutorRequest.Builder request, String gav, @Nullable String repositoryId)
            throws ExecutorException;
}
