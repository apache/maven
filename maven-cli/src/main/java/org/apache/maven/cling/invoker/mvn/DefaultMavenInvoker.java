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
package org.apache.maven.cling.invoker.mvn;

import java.io.FileNotFoundException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Date;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.maven.InternalErrorException;
import org.apache.maven.Maven;
import org.apache.maven.api.Constants;
import org.apache.maven.api.cli.Logger;
import org.apache.maven.api.cli.mvn.MavenInvoker;
import org.apache.maven.api.cli.mvn.MavenInvokerRequest;
import org.apache.maven.api.cli.mvn.MavenOptions;
import org.apache.maven.building.FileSource;
import org.apache.maven.building.Problem;
import org.apache.maven.cli.CLIReportingUtils;
import org.apache.maven.cli.event.DefaultEventSpyContext;
import org.apache.maven.cli.event.ExecutionEventLogger;
import org.apache.maven.cling.invoker.LookupInvoker;
import org.apache.maven.cling.invoker.ProtoLookup;
import org.apache.maven.eventspy.internal.EventSpyDispatcher;
import org.apache.maven.exception.DefaultExceptionHandler;
import org.apache.maven.exception.ExceptionHandler;
import org.apache.maven.exception.ExceptionSummary;
import org.apache.maven.execution.DefaultMavenExecutionRequest;
import org.apache.maven.execution.ExecutionListener;
import org.apache.maven.execution.MavenExecutionRequest;
import org.apache.maven.execution.MavenExecutionRequestPopulator;
import org.apache.maven.execution.MavenExecutionResult;
import org.apache.maven.execution.ProfileActivation;
import org.apache.maven.execution.ProjectActivation;
import org.apache.maven.jline.MessageUtils;
import org.apache.maven.lifecycle.LifecycleExecutionException;
import org.apache.maven.model.building.ModelProcessor;
import org.apache.maven.project.MavenProject;
import org.apache.maven.settings.building.SettingsBuildingRequest;
import org.apache.maven.settings.building.SettingsBuildingResult;
import org.apache.maven.toolchain.building.DefaultToolchainsBuildingRequest;
import org.apache.maven.toolchain.building.ToolchainsBuilder;
import org.apache.maven.toolchain.building.ToolchainsBuildingResult;
import org.codehaus.plexus.PlexusContainer;
import org.eclipse.aether.DefaultRepositoryCache;

import static java.util.Comparator.comparing;
import static org.apache.maven.cling.invoker.Utils.toProperties;

public abstract class DefaultMavenInvoker<
                O extends MavenOptions,
                R extends MavenInvokerRequest<O>,
                C extends DefaultMavenInvoker.MavenContext<O, R, C>>
        extends LookupInvoker<O, R, C> implements MavenInvoker<R> {

    @SuppressWarnings("VisibilityModifier")
    protected static class MavenContext<
                    O extends MavenOptions,
                    R extends MavenInvokerRequest<O>,
                    C extends DefaultMavenInvoker.MavenContext<O, R, C>>
            extends LookupInvokerContext<O, R, C> {
        protected MavenContext(DefaultMavenInvoker<O, R, C> invoker, R invokerRequest) {
            super(invoker, invokerRequest);
        }

        public MavenExecutionRequest mavenExecutionRequest;
        public EventSpyDispatcher eventSpyDispatcher;
        public MavenExecutionRequestPopulator mavenExecutionRequestPopulator;
        public ToolchainsBuilder toolchainsBuilder;
        public ModelProcessor modelProcessor;
        public Maven maven;
    }

    public DefaultMavenInvoker(ProtoLookup protoLookup) {
        super(protoLookup);
    }

    @Override
    protected int execute(C context) throws Exception {
        toolchains(context);
        populateRequest(context, context.mavenExecutionRequest);
        return doExecute(context);
    }

    @Override
    protected void prepare(C localContext) throws Exception {
        // explicitly fill in "defaults"?
        DefaultMavenExecutionRequest mavenExecutionRequest = new DefaultMavenExecutionRequest();
        mavenExecutionRequest.setRepositoryCache(new DefaultRepositoryCache());
        mavenExecutionRequest.setInteractiveMode(true);
        mavenExecutionRequest.setIgnoreInvalidArtifactDescriptor(true);
        mavenExecutionRequest.setIgnoreMissingArtifactDescriptor(true);
        mavenExecutionRequest.setProjectPresent(true);
        mavenExecutionRequest.setRecursive(true);
        mavenExecutionRequest.setReactorFailureBehavior(MavenExecutionRequest.REACTOR_FAIL_FAST);
        mavenExecutionRequest.setStartTime(new Date());
        mavenExecutionRequest.setLoggingLevel(MavenExecutionRequest.LOGGING_LEVEL_INFO);
        mavenExecutionRequest.setDegreeOfConcurrency(1);
        mavenExecutionRequest.setBuilderId("singlethreaded");

        localContext.mavenExecutionRequest = mavenExecutionRequest;
    }

    @Override
    protected void lookup(C context) throws Exception {
        context.eventSpyDispatcher = context.lookup.lookup(EventSpyDispatcher.class);
        context.mavenExecutionRequestPopulator = context.lookup.lookup(MavenExecutionRequestPopulator.class);
        context.toolchainsBuilder = context.lookup.lookup(ToolchainsBuilder.class);
        context.modelProcessor = context.lookup.lookup(ModelProcessor.class);
        context.maven = context.lookup.lookup(Maven.class);
    }

    @Override
    protected void init(C context) throws Exception {
        MavenInvokerRequest<O> invokerRequest = context.invokerRequest;
        DefaultEventSpyContext eventSpyContext = new DefaultEventSpyContext();
        Map<String, Object> data = eventSpyContext.getData();
        data.put("plexus", context.lookup.lookup(PlexusContainer.class));
        data.put("workingDirectory", invokerRequest.cwd().toString());
        data.put("systemProperties", toProperties(invokerRequest.systemProperties()));
        data.put("userProperties", toProperties(invokerRequest.userProperties()));
        data.put("versionProperties", CLIReportingUtils.getBuildProperties());
        context.eventSpyDispatcher.init(eventSpyContext);
    }

    @Override
    protected void postCommands(C localContext) throws Exception {
        super.postCommands(localContext);

        R invokerRequest = localContext.invokerRequest;
        Logger logger = localContext.logger;
        if (invokerRequest.options().relaxedChecksums().orElse(false)) {
            logger.info("Disabling strict checksum verification on all artifact downloads.");
        } else if (invokerRequest.options().strictChecksums().orElse(false)) {
            logger.info("Enabling strict checksum verification on all artifact downloads.");
        }
    }

    @Override
    protected void customizeSettingsRequest(C context, SettingsBuildingRequest settingsBuildingRequest) {
        if (context.eventSpyDispatcher != null) {
            context.eventSpyDispatcher.onEvent(settingsBuildingRequest);
        }
    }

    @Override
    protected void customizeSettingsResult(C context, SettingsBuildingResult settingsBuildingResult) {
        if (context.eventSpyDispatcher != null) {
            context.eventSpyDispatcher.onEvent(settingsBuildingResult);
        }
    }

    protected void toolchains(C localContext) throws Exception {
        Path userToolchainsFile = null;

        if (localContext.invokerRequest.options().altUserToolchains().isPresent()) {
            userToolchainsFile = localContext.cwdResolver.apply(
                    localContext.invokerRequest.options().altUserToolchains().get());

            if (!Files.isRegularFile(userToolchainsFile)) {
                throw new FileNotFoundException(
                        "The specified user toolchains file does not exist: " + userToolchainsFile);
            }
        } else {
            String userToolchainsFileStr =
                    localContext.invokerRequest.userProperties().get(Constants.MAVEN_USER_TOOLCHAINS);
            if (userToolchainsFileStr != null) {
                userToolchainsFile = localContext.cwdResolver.apply(userToolchainsFileStr);
            }
        }

        Path installationToolchainsFile = null;

        if (localContext.invokerRequest.options().altInstallationToolchains().isPresent()) {
            installationToolchainsFile = localContext.cwdResolver.apply(localContext
                    .invokerRequest
                    .options()
                    .altInstallationToolchains()
                    .get());

            if (!Files.isRegularFile(installationToolchainsFile)) {
                throw new FileNotFoundException(
                        "The specified installation toolchains file does not exist: " + installationToolchainsFile);
            }
        } else {
            String installationToolchainsFileStr =
                    localContext.invokerRequest.userProperties().get(Constants.MAVEN_INSTALLATION_TOOLCHAINS);
            if (installationToolchainsFileStr != null) {
                installationToolchainsFile = localContext.cwdResolver.apply(installationToolchainsFileStr);
            }
        }

        localContext.mavenExecutionRequest.setInstallationToolchainsFile(
                installationToolchainsFile != null ? installationToolchainsFile.toFile() : null);
        localContext.mavenExecutionRequest.setUserToolchainsFile(
                userToolchainsFile != null ? userToolchainsFile.toFile() : null);

        DefaultToolchainsBuildingRequest toolchainsRequest = new DefaultToolchainsBuildingRequest();
        if (installationToolchainsFile != null && Files.isRegularFile(installationToolchainsFile)) {
            toolchainsRequest.setGlobalToolchainsSource(new FileSource(installationToolchainsFile));
        }
        if (userToolchainsFile != null && Files.isRegularFile(userToolchainsFile)) {
            toolchainsRequest.setUserToolchainsSource(new FileSource(userToolchainsFile));
        }

        localContext.eventSpyDispatcher.onEvent(toolchainsRequest);

        localContext.logger.debug("Reading installation toolchains from '"
                + (toolchainsRequest.getGlobalToolchainsSource() != null
                        ? toolchainsRequest.getGlobalToolchainsSource().getLocation()
                        : installationToolchainsFile)
                + "'");
        localContext.logger.debug("Reading user toolchains from '"
                + (toolchainsRequest.getUserToolchainsSource() != null
                        ? toolchainsRequest.getUserToolchainsSource().getLocation()
                        : userToolchainsFile)
                + "'");

        ToolchainsBuildingResult toolchainsResult = localContext.toolchainsBuilder.build(toolchainsRequest);

        localContext.eventSpyDispatcher.onEvent(toolchainsResult);

        localContext.mavenExecutionRequestPopulator.populateFromToolchains(
                localContext.mavenExecutionRequest, toolchainsResult.getEffectiveToolchains());

        if (!toolchainsResult.getProblems().isEmpty()) {
            localContext.logger.warn("");
            localContext.logger.warn("Some problems were encountered while building the effective toolchains");

            for (Problem problem : toolchainsResult.getProblems()) {
                localContext.logger.warn(problem.getMessage() + " @ " + problem.getLocation());
            }

            localContext.logger.warn("");
        }
    }

    @Override
    protected void populateRequest(C context, MavenExecutionRequest request) throws Exception {
        super.populateRequest(context, request);

        MavenOptions options = context.invokerRequest.options();
        request.setNoSnapshotUpdates(options.suppressSnapshotUpdates().orElse(false));
        request.setGoals(options.goals().orElse(List.of()));
        request.setReactorFailureBehavior(determineReactorFailureBehaviour(context));
        request.setRecursive(!options.nonRecursive().orElse(!request.isRecursive()));
        request.setOffline(options.offline().orElse(request.isOffline()));
        request.setUpdateSnapshots(options.updateSnapshots().orElse(false));
        request.setGlobalChecksumPolicy(determineGlobalChecksumPolicy(context));

        Path pom = determinePom(context);
        request.setPom(pom != null ? pom.toFile() : null);
        request.setTransferListener(
                determineTransferListener(context, options.noTransferProgress().orElse(false)));
        request.setExecutionListener(determineExecutionListener(context));

        if ((request.getPom() != null) && (request.getPom().getParentFile() != null)) {
            request.setBaseDirectory(request.getPom().getParentFile());
        }

        request.setResumeFrom(options.resumeFrom().orElse(null));
        request.setResume(options.resume().orElse(false));
        request.setMakeBehavior(determineMakeBehavior(context));
        request.setCacheNotFound(options.cacheArtifactNotFound().orElse(true));
        request.setCacheTransferError(false);

        if (options.strictArtifactDescriptorPolicy().orElse(false)) {
            request.setIgnoreMissingArtifactDescriptor(false);
            request.setIgnoreInvalidArtifactDescriptor(false);
        } else {
            request.setIgnoreMissingArtifactDescriptor(true);
            request.setIgnoreInvalidArtifactDescriptor(true);
        }

        request.setIgnoreTransitiveRepositories(
                options.ignoreTransitiveRepositories().orElse(false));

        performProjectActivation(context, request.getProjectActivation());
        performProfileActivation(context, request.getProfileActivation());

        //
        // Builder, concurrency and parallelism
        //
        // We preserve the existing methods for builder selection which is to look for various inputs in the threading
        // configuration. We don't have an easy way to allow a pluggable builder to provide its own configuration
        // parameters but this is sufficient for now. Ultimately we want components like Builders to provide a way to
        // extend the command line to accept its own configuration parameters.
        //
        if (context.invokerRequest.options().threads().isPresent()) {
            int degreeOfConcurrency = calculateDegreeOfConcurrency(
                    context.invokerRequest.options().threads().get());
            if (degreeOfConcurrency > 1) {
                request.setBuilderId("multithreaded");
                request.setDegreeOfConcurrency(degreeOfConcurrency);
            }
        }

        //
        // Allow the builder to be overridden by the user if requested. The builders are now pluggable.
        //
        if (context.invokerRequest.options().builder().isPresent()) {
            request.setBuilderId(context.invokerRequest.options().builder().get());
        }
    }

    protected Path determinePom(C localContext) {
        Path current = localContext.invokerRequest.cwd();
        if (localContext.invokerRequest.options().alternatePomFile().isPresent()) {
            current = localContext.cwdResolver.apply(
                    localContext.invokerRequest.options().alternatePomFile().get());
        }
        if (localContext.modelProcessor != null) {
            return localContext.modelProcessor.locateExistingPom(current);
        } else {
            return Files.isRegularFile(current) ? current : null;
        }
    }

    protected String determineReactorFailureBehaviour(C localContext) {
        MavenOptions mavenOptions = localContext.invokerRequest.options();
        if (mavenOptions.failFast().isPresent()) {
            return MavenExecutionRequest.REACTOR_FAIL_FAST;
        } else if (mavenOptions.failAtEnd().isPresent()) {
            return MavenExecutionRequest.REACTOR_FAIL_AT_END;
        } else if (mavenOptions.failNever().isPresent()) {
            return MavenExecutionRequest.REACTOR_FAIL_NEVER;
        } else {
            return MavenExecutionRequest.REACTOR_FAIL_FAST;
        }
    }

    protected String determineGlobalChecksumPolicy(C localContext) {
        MavenOptions mavenOptions = localContext.invokerRequest.options();
        if (mavenOptions.strictChecksums().orElse(false)) {
            return MavenExecutionRequest.CHECKSUM_POLICY_FAIL;
        } else if (mavenOptions.relaxedChecksums().orElse(false)) {
            return MavenExecutionRequest.CHECKSUM_POLICY_WARN;
        } else {
            return null;
        }
    }

    protected ExecutionListener determineExecutionListener(C localContext) {
        ExecutionListener executionListener =
                new ExecutionEventLogger(localContext.invokerRequest.messageBuilderFactory());
        if (localContext.eventSpyDispatcher != null) {
            return localContext.eventSpyDispatcher.chainListener(executionListener);
        } else {
            return executionListener;
        }
    }

    protected String determineMakeBehavior(C localContext) {
        MavenOptions mavenOptions = localContext.invokerRequest.options();
        if (mavenOptions.alsoMake().isPresent()
                && mavenOptions.alsoMakeDependents().isEmpty()) {
            return MavenExecutionRequest.REACTOR_MAKE_UPSTREAM;
        } else if (mavenOptions.alsoMake().isEmpty()
                && mavenOptions.alsoMakeDependents().isPresent()) {
            return MavenExecutionRequest.REACTOR_MAKE_DOWNSTREAM;
        } else if (mavenOptions.alsoMake().isPresent()
                && mavenOptions.alsoMakeDependents().isPresent()) {
            return MavenExecutionRequest.REACTOR_MAKE_BOTH;
        } else {
            return null;
        }
    }

    protected void performProjectActivation(C localContext, ProjectActivation projectActivation) {
        MavenOptions mavenOptions = localContext.invokerRequest.options();
        if (mavenOptions.projects().isPresent()
                && !mavenOptions.projects().get().isEmpty()) {
            List<String> optionValues = mavenOptions.projects().get();
            for (final String optionValue : optionValues) {
                for (String token : optionValue.split(",")) {
                    String selector = token.trim();
                    boolean active = true;
                    if (!selector.isEmpty()) {
                        if (selector.charAt(0) == '-' || selector.charAt(0) == '!') {
                            active = false;
                            selector = selector.substring(1);
                        } else if (token.charAt(0) == '+') {
                            selector = selector.substring(1);
                        }
                    }
                    boolean optional = false;
                    if (!selector.isEmpty() && selector.charAt(0) == '?') {
                        optional = true;
                        selector = selector.substring(1);
                    }
                    projectActivation.addProjectActivation(selector, active, optional);
                }
            }
        }
    }

    protected void performProfileActivation(C localContext, ProfileActivation profileActivation) {
        MavenOptions mavenOptions = localContext.invokerRequest.options();
        if (mavenOptions.activatedProfiles().isPresent()
                && !mavenOptions.activatedProfiles().get().isEmpty()) {
            List<String> optionValues = mavenOptions.activatedProfiles().get();
            for (final String optionValue : optionValues) {
                for (String token : optionValue.split(",")) {
                    String profileId = token.trim();
                    boolean active = true;
                    if (!profileId.isEmpty()) {
                        if (profileId.charAt(0) == '-' || profileId.charAt(0) == '!') {
                            active = false;
                            profileId = profileId.substring(1);
                        } else if (token.charAt(0) == '+') {
                            profileId = profileId.substring(1);
                        }
                    }
                    boolean optional = false;
                    if (!profileId.isEmpty() && profileId.charAt(0) == '?') {
                        optional = true;
                        profileId = profileId.substring(1);
                    }
                    profileActivation.addProfileActivation(profileId, active, optional);
                }
            }
        }
    }

    protected int doExecute(C localContext) throws Exception {
        MavenExecutionRequest request = localContext.mavenExecutionRequest;

        // why? No way to disable caching?
        if (localContext.mavenExecutionRequest.getRepositoryCache() == null) {
            localContext.mavenExecutionRequest.setRepositoryCache(new DefaultRepositoryCache());
        }

        localContext.eventSpyDispatcher.onEvent(request);

        MavenExecutionResult result = localContext.maven.execute(request);

        localContext.eventSpyDispatcher.onEvent(result);

        localContext.eventSpyDispatcher.close();

        if (result.hasExceptions()) {
            ExceptionHandler handler = new DefaultExceptionHandler();

            Map<String, String> references = new LinkedHashMap<>();

            List<MavenProject> failedProjects = new ArrayList<>();

            for (Throwable exception : result.getExceptions()) {
                ExceptionSummary summary = handler.handleException(exception);

                logSummary(localContext, summary, references, "");

                if (exception instanceof LifecycleExecutionException) {
                    failedProjects.add(((LifecycleExecutionException) exception).getProject());
                }
            }

            localContext.logger.error("");

            if (!localContext.invokerRequest.options().showErrors().orElse(false)) {
                localContext.logger.error("To see the full stack trace of the errors, re-run Maven with the '"
                        + MessageUtils.builder().strong("-e") + "' switch");
            }
            if (!localContext.invokerRequest.options().verbose().orElse(false)) {
                localContext.logger.error("Re-run Maven using the '"
                        + MessageUtils.builder().strong("-X") + "' switch to enable verbose output");
            }

            if (!references.isEmpty()) {
                localContext.logger.error("");
                localContext.logger.error("For more information about the errors and possible solutions"
                        + ", please read the following articles:");

                for (Map.Entry<String, String> entry : references.entrySet()) {
                    localContext.logger.error(MessageUtils.builder().strong(entry.getValue()) + " " + entry.getKey());
                }
            }

            if (result.canResume()) {
                logBuildResumeHint(localContext, "mvn [args] -r");
            } else if (!failedProjects.isEmpty()) {
                List<MavenProject> sortedProjects = result.getTopologicallySortedProjects();

                // Sort the failedProjects list in the topologically sorted order.
                failedProjects.sort(comparing(sortedProjects::indexOf));

                MavenProject firstFailedProject = failedProjects.get(0);
                if (!firstFailedProject.equals(sortedProjects.get(0))) {
                    String resumeFromSelector = getResumeFromSelector(sortedProjects, firstFailedProject);
                    logBuildResumeHint(localContext, "mvn [args] -rf " + resumeFromSelector);
                }
            }

            if (localContext.invokerRequest.options().failNever().orElse(false)) {
                localContext.logger.info("Build failures were ignored.");
                return 0;
            } else {
                return 1;
            }
        } else {
            return 0;
        }
    }

    protected void logBuildResumeHint(C localContext, String resumeBuildHint) {
        localContext.logger.error("");
        localContext.logger.error("After correcting the problems, you can resume the build with the command");
        localContext.logger.error(
                MessageUtils.builder().a("  ").strong(resumeBuildHint).toString());
    }

    /**
     * A helper method to determine the value to resume the build with {@code -rf} taking into account the edge case
     *   where multiple modules in the reactor have the same artifactId.
     * <p>
     * {@code -rf :artifactId} will pick up the first module which matches, but when multiple modules in the reactor
     *   have the same artifactId, effective failed module might be later in build reactor.
     * This means that developer will either have to type groupId or wait for build execution of all modules which
     *   were fine, but they are still before one which reported errors.
     * <p>Then the returned value is {@code groupId:artifactId} when there is a name clash and
     * {@code :artifactId} if there is no conflict.
     * This method is made package-private for testing purposes.
     *
     * @param mavenProjects Maven projects which are part of build execution.
     * @param firstFailedProject The first project which has failed.
     * @return Value for -rf flag to resume build exactly from place where it failed ({@code :artifactId} in general
     * and {@code groupId:artifactId} when there is a name clash).
     */
    protected String getResumeFromSelector(List<MavenProject> mavenProjects, MavenProject firstFailedProject) {
        boolean hasOverlappingArtifactId = mavenProjects.stream()
                        .filter(project -> firstFailedProject.getArtifactId().equals(project.getArtifactId()))
                        .count()
                > 1;

        if (hasOverlappingArtifactId) {
            return firstFailedProject.getGroupId() + ":" + firstFailedProject.getArtifactId();
        }

        return ":" + firstFailedProject.getArtifactId();
    }

    protected static final Pattern NEXT_LINE = Pattern.compile("\r?\n");

    protected static final Pattern LAST_ANSI_SEQUENCE = Pattern.compile("(\u001B\\[[;\\d]*[ -/]*[@-~])[^\u001B]*$");

    protected static final String ANSI_RESET = "\u001B\u005Bm";

    protected void logSummary(C localContext, ExceptionSummary summary, Map<String, String> references, String indent) {
        String referenceKey = "";

        if (summary.getReference() != null && !summary.getReference().isEmpty()) {
            referenceKey =
                    references.computeIfAbsent(summary.getReference(), k -> "[Help " + (references.size() + 1) + "]");
        }

        String msg = summary.getMessage();

        if (!referenceKey.isEmpty()) {
            if (msg.indexOf('\n') < 0) {
                msg += " -> " + MessageUtils.builder().strong(referenceKey);
            } else {
                msg += "\n-> " + MessageUtils.builder().strong(referenceKey);
            }
        }

        String[] lines = NEXT_LINE.split(msg);
        String currentColor = "";

        for (int i = 0; i < lines.length; i++) {
            // add eventual current color inherited from previous line
            String line = currentColor + lines[i];

            // look for last ANSI escape sequence to check if nextColor
            Matcher matcher = LAST_ANSI_SEQUENCE.matcher(line);
            String nextColor = "";
            if (matcher.find()) {
                nextColor = matcher.group(1);
                if (ANSI_RESET.equals(nextColor)) {
                    // last ANSI escape code is reset: no next color
                    nextColor = "";
                }
            }

            // effective line, with indent and reset if end is colored
            line = indent + line + ("".equals(nextColor) ? "" : ANSI_RESET);

            if ((i == lines.length - 1)
                    && (localContext.invokerRequest.options().showErrors().orElse(false)
                            || (summary.getException() instanceof InternalErrorException))) {
                localContext.logger.error(line, summary.getException());
            } else {
                localContext.logger.error(line);
            }

            currentColor = nextColor;
        }

        indent += "  ";

        for (ExceptionSummary child : summary.getChildren()) {
            logSummary(localContext, child, references, indent);
        }
    }
}
