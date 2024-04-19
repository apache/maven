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
package org.apache.maven.execution;

import java.io.File;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.Properties;

import org.apache.maven.artifact.repository.ArtifactRepository;
import org.apache.maven.artifact.repository.ArtifactRepositoryPolicy;
import org.apache.maven.eventspy.internal.EventSpyDispatcher;
import org.apache.maven.model.Profile;
import org.apache.maven.project.ProjectBuildingRequest;
import org.apache.maven.settings.Mirror;
import org.apache.maven.settings.Proxy;
import org.apache.maven.settings.Server;
import org.apache.maven.toolchain.model.ToolchainModel;
import org.codehaus.plexus.logging.Logger;
import org.eclipse.aether.RepositoryCache;
import org.eclipse.aether.repository.WorkspaceReader;
import org.eclipse.aether.transfer.TransferListener;

/**
 * @author Jason van Zyl
 */
public interface MavenExecutionRequest {
    // ----------------------------------------------------------------------
    // Logging
    // ----------------------------------------------------------------------

    int LOGGING_LEVEL_DEBUG = Logger.LEVEL_DEBUG;

    int LOGGING_LEVEL_INFO = Logger.LEVEL_INFO;

    int LOGGING_LEVEL_WARN = Logger.LEVEL_WARN;

    int LOGGING_LEVEL_ERROR = Logger.LEVEL_ERROR;

    int LOGGING_LEVEL_FATAL = Logger.LEVEL_FATAL;

    int LOGGING_LEVEL_DISABLED = Logger.LEVEL_DISABLED;

    // ----------------------------------------------------------------------
    // Reactor Failure Mode
    // ----------------------------------------------------------------------

    String REACTOR_FAIL_FAST = "FAIL_FAST";

    String REACTOR_FAIL_AT_END = "FAIL_AT_END";

    String REACTOR_FAIL_NEVER = "FAIL_NEVER";

    // ----------------------------------------------------------------------
    // Reactor Make Mode
    // ----------------------------------------------------------------------

    String REACTOR_MAKE_UPSTREAM = "make-upstream";

    String REACTOR_MAKE_DOWNSTREAM = "make-downstream";

    String REACTOR_MAKE_BOTH = "make-both";

    // ----------------------------------------------------------------------
    // Artifact repository policies
    // ----------------------------------------------------------------------

    String CHECKSUM_POLICY_FAIL = ArtifactRepositoryPolicy.CHECKSUM_POLICY_FAIL;

    String CHECKSUM_POLICY_WARN = ArtifactRepositoryPolicy.CHECKSUM_POLICY_WARN;

    // ----------------------------------------------------------------------
    //
    // ----------------------------------------------------------------------

    // Base directory
    MavenExecutionRequest setBaseDirectory(File basedir);

    String getBaseDirectory();

    // Timing (remove this)
    MavenExecutionRequest setStartTime(Date start);

    Date getStartTime();

    // Goals
    MavenExecutionRequest setGoals(List<String> goals);

    List<String> getGoals();

    // Properties

    /**
     * Sets the system properties to use for interpolation and profile activation. The system properties are collected
     * from the runtime environment like {@link System#getProperties()} and environment variables.
     *
     * @param systemProperties The system properties, may be {@code null}.
     * @return This request, never {@code null}.
     */
    MavenExecutionRequest setSystemProperties(Properties systemProperties);

    /**
     * Gets the system properties to use for interpolation and profile activation. The system properties are collected
     * from the runtime environment like {@link System#getProperties()} and environment variables.
     *
     * @return The system properties, never {@code null}.
     */
    Properties getSystemProperties();

    /**
     * Sets the user properties to use for interpolation and profile activation. The user properties have been
     * configured directly by the user on his discretion, e.g. via the {@code -Dkey=value} parameter on the command
     * line.
     *
     * @param userProperties The user properties, may be {@code null}.
     * @return This request, never {@code null}.
     */
    MavenExecutionRequest setUserProperties(Properties userProperties);

    /**
     * Gets the user properties to use for interpolation and profile activation. The user properties have been
     * configured directly by the user on his discretion, e.g. via the {@code -Dkey=value} parameter on the command
     * line.
     *
     * @return The user properties, never {@code null}.
     */
    Properties getUserProperties();

    // Reactor
    MavenExecutionRequest setReactorFailureBehavior(String failureBehavior);

    String getReactorFailureBehavior();

    MavenExecutionRequest setSelectedProjects(List<String> projects);

    List<String> getSelectedProjects();

    /**
     * @param projects the projects to exclude
     * @return this MavenExecutionRequest
     * @since 3.2
     */
    MavenExecutionRequest setExcludedProjects(List<String> projects);

    /**
     * @return the excluded projects, never {@code null}
     * @since 3.2
     */
    List<String> getExcludedProjects();

    MavenExecutionRequest setResumeFrom(String project);

    String getResumeFrom();

    MavenExecutionRequest setMakeBehavior(String makeBehavior);

    String getMakeBehavior();

    /**
     * Set's the parallel degree of concurrency used by the build.
     *
     * @param degreeOfConcurrency
     */
    void setDegreeOfConcurrency(int degreeOfConcurrency);

    /**
     * @return the degree of concurrency for the build.
     */
    int getDegreeOfConcurrency();

    // Recursive (really to just process the top-level POM)
    MavenExecutionRequest setRecursive(boolean recursive);

    boolean isRecursive();

    MavenExecutionRequest setPom(File pom);

    File getPom();

    // Errors
    MavenExecutionRequest setShowErrors(boolean showErrors);

    boolean isShowErrors();

    // Transfer listeners
    MavenExecutionRequest setTransferListener(TransferListener transferListener);

    TransferListener getTransferListener();

    // Logging
    MavenExecutionRequest setLoggingLevel(int loggingLevel);

    int getLoggingLevel();

    // Update snapshots
    MavenExecutionRequest setUpdateSnapshots(boolean updateSnapshots);

    boolean isUpdateSnapshots();

    MavenExecutionRequest setNoSnapshotUpdates(boolean noSnapshotUpdates);

    boolean isNoSnapshotUpdates();

    // Checksum policy
    MavenExecutionRequest setGlobalChecksumPolicy(String globalChecksumPolicy);

    String getGlobalChecksumPolicy();

    // Local repository
    MavenExecutionRequest setLocalRepositoryPath(String localRepository);

    MavenExecutionRequest setLocalRepositoryPath(File localRepository);

    File getLocalRepositoryPath();

    MavenExecutionRequest setLocalRepository(ArtifactRepository repository);

    ArtifactRepository getLocalRepository();

    // Interactive
    MavenExecutionRequest setInteractiveMode(boolean interactive);

    boolean isInteractiveMode();

    // Offline
    MavenExecutionRequest setOffline(boolean offline);

    boolean isOffline();

    boolean isCacheTransferError();

    MavenExecutionRequest setCacheTransferError(boolean cacheTransferError);

    boolean isCacheNotFound();

    MavenExecutionRequest setCacheNotFound(boolean cacheNotFound);

    /**
     * @since 3.9.7
     */
    boolean isIgnoreTransitiveRepositories();

    /**
     * @since 3.9.7
     */
    MavenExecutionRequest setIgnoreTransitiveRepositories(boolean ignoreTransitiveRepositories);

    // Profiles
    List<Profile> getProfiles();

    MavenExecutionRequest addProfile(Profile profile);

    MavenExecutionRequest setProfiles(List<Profile> profiles);

    MavenExecutionRequest addActiveProfile(String profile);

    MavenExecutionRequest addActiveProfiles(List<String> profiles);

    MavenExecutionRequest setActiveProfiles(List<String> profiles);

    List<String> getActiveProfiles();

    MavenExecutionRequest addInactiveProfile(String profile);

    MavenExecutionRequest addInactiveProfiles(List<String> profiles);

    MavenExecutionRequest setInactiveProfiles(List<String> profiles);

    List<String> getInactiveProfiles();

    // Proxies
    List<Proxy> getProxies();

    MavenExecutionRequest setProxies(List<Proxy> proxies);

    MavenExecutionRequest addProxy(Proxy proxy);

    // Servers
    List<Server> getServers();

    MavenExecutionRequest setServers(List<Server> servers);

    MavenExecutionRequest addServer(Server server);

    // Mirrors
    List<Mirror> getMirrors();

    MavenExecutionRequest setMirrors(List<Mirror> mirrors);

    MavenExecutionRequest addMirror(Mirror mirror);

    // Plugin groups
    List<String> getPluginGroups();

    MavenExecutionRequest setPluginGroups(List<String> pluginGroups);

    MavenExecutionRequest addPluginGroup(String pluginGroup);

    MavenExecutionRequest addPluginGroups(List<String> pluginGroups);

    boolean isProjectPresent();

    MavenExecutionRequest setProjectPresent(boolean isProjectPresent);

    File getUserSettingsFile();

    MavenExecutionRequest setUserSettingsFile(File userSettingsFile);

    File getGlobalSettingsFile();

    MavenExecutionRequest setGlobalSettingsFile(File globalSettingsFile);

    MavenExecutionRequest addRemoteRepository(ArtifactRepository repository);

    MavenExecutionRequest addPluginArtifactRepository(ArtifactRepository repository);

    /**
     * Set a new list of remote repositories to use the execution request. This is necessary if you perform
     * transformations on the remote repositories being used. For example if you replace existing repositories with
     * mirrors then it's easier to just replace the whole list with a new list of transformed repositories.
     *
     * @param repositories
     * @return This request, never {@code null}.
     */
    MavenExecutionRequest setRemoteRepositories(List<ArtifactRepository> repositories);

    List<ArtifactRepository> getRemoteRepositories();

    MavenExecutionRequest setPluginArtifactRepositories(List<ArtifactRepository> repositories);

    List<ArtifactRepository> getPluginArtifactRepositories();

    MavenExecutionRequest setRepositoryCache(RepositoryCache repositoryCache);

    RepositoryCache getRepositoryCache();

    WorkspaceReader getWorkspaceReader();

    MavenExecutionRequest setWorkspaceReader(WorkspaceReader workspaceReader);

    File getUserToolchainsFile();

    MavenExecutionRequest setUserToolchainsFile(File userToolchainsFile);

    /**
     *
     *
     * @return the global toolchains file
     * @since 3.3.0
     */
    File getGlobalToolchainsFile();

    /**
     *
     * @param globalToolchainsFile the global toolchains file
     * @return this request
     * @since 3.3.0
     */
    MavenExecutionRequest setGlobalToolchainsFile(File globalToolchainsFile);

    ExecutionListener getExecutionListener();

    MavenExecutionRequest setExecutionListener(ExecutionListener executionListener);

    ProjectBuildingRequest getProjectBuildingRequest();

    /**
     * @since 3.1
     * @deprecated Since 3.9 there is no direct Maven2 interop offered at LRM level. See
     * <a href="https://maven.apache.org/resolver/configuration.html">Resolver Configuration</a> page option
     * {@code aether.artifactResolver.simpleLrmInterop} that provides similar semantics. This method should
     * be never invoked, and always returns {@code false}.
     */
    @Deprecated
    boolean isUseLegacyLocalRepository();

    /**
     * @since 3.1
     * @deprecated Since 3.9 there is no direct Maven2 interop offered at LRM level. See
     * <a href="https://maven.apache.org/resolver/configuration.html">Resolver Configuration</a> page option
     * {@code aether.artifactResolver.simpleLrmInterop} that provides similar semantics. This method should
     * be never invoked, and ignores parameter (value remains always {@code false}).     */
    @Deprecated
    MavenExecutionRequest setUseLegacyLocalRepository(boolean useLegacyLocalRepository);

    /**
     * Controls the {@link org.apache.maven.lifecycle.internal.builder.Builder} used by Maven by specification
     * of the builder's id.
     *
     * @since 3.2.0
     */
    MavenExecutionRequest setBuilderId(String builderId);

    /**
     * Controls the {@link org.apache.maven.lifecycle.internal.builder.Builder} used by Maven by specification
     * of the builders id.
     *
     * @since 3.2.0
     */
    String getBuilderId();

    /**
     *
     * @param toolchains all toolchains grouped by type
     * @return this request
     * @since 3.3.0
     */
    MavenExecutionRequest setToolchains(Map<String, List<ToolchainModel>> toolchains);

    /**
     *
     * @return all toolchains grouped by type, never {@code null}
     * @since 3.3.0
     */
    Map<String, List<ToolchainModel>> getToolchains();

    /**
     * @since 3.3.0
     */
    void setMultiModuleProjectDirectory(File file);

    /**
     * @since 3.3.0
     */
    File getMultiModuleProjectDirectory();

    /**
     * @since 3.3.0
     */
    MavenExecutionRequest setEventSpyDispatcher(EventSpyDispatcher eventSpyDispatcher);

    /**
     * @since 3.3.0
     */
    EventSpyDispatcher getEventSpyDispatcher();

    /**
     * @since 3.3.0
     */
    Map<String, Object> getData();
}
