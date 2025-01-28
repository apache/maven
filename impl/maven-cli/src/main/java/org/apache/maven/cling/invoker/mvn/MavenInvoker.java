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
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.maven.InternalErrorException;
import org.apache.maven.Maven;
import org.apache.maven.api.Constants;
import org.apache.maven.api.MonotonicClock;
import org.apache.maven.api.annotations.Nullable;
import org.apache.maven.api.cli.InvokerException;
import org.apache.maven.api.cli.InvokerRequest;
import org.apache.maven.api.cli.Logger;
import org.apache.maven.api.cli.mvn.MavenOptions;
import org.apache.maven.api.services.BuilderProblem;
import org.apache.maven.api.services.Lookup;
import org.apache.maven.api.services.PathSource;
import org.apache.maven.api.services.ToolchainsBuilder;
import org.apache.maven.api.services.ToolchainsBuilderRequest;
import org.apache.maven.api.services.ToolchainsBuilderResult;
import org.apache.maven.api.services.model.ModelProcessor;
import org.apache.maven.cling.event.ExecutionEventLogger;
import org.apache.maven.cling.invoker.LookupContext;
import org.apache.maven.cling.invoker.LookupInvoker;
import org.apache.maven.cling.invoker.Utils;
import org.apache.maven.cling.transfer.ConsoleMavenTransferListener;
import org.apache.maven.cling.transfer.QuietMavenTransferListener;
import org.apache.maven.cling.transfer.SimplexTransferListener;
import org.apache.maven.cling.transfer.Slf4jMavenTransferListener;
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
import org.apache.maven.logging.LoggingExecutionListener;
import org.apache.maven.logging.MavenTransferListener;
import org.apache.maven.project.MavenProject;
import org.eclipse.aether.DefaultRepositoryCache;
import org.eclipse.aether.transfer.TransferListener;

import static java.util.Comparator.comparing;

/**
 * The Maven invoker, that expects whole Maven on classpath and invokes it.
 */
public class MavenInvoker extends LookupInvoker<MavenContext> {
    public MavenInvoker(Lookup protoLookup) {
        this(protoLookup, null);
    }

    public MavenInvoker(Lookup protoLookup, @Nullable Consumer<LookupContext> contextConsumer) {
        super(protoLookup, contextConsumer);
    }

    @Override
    protected MavenContext createContext(InvokerRequest invokerRequest) throws InvokerException {
        return new MavenContext(invokerRequest);
    }

    @Override
    protected int execute(MavenContext context) throws Exception {
        MavenExecutionRequest request = prepareMavenExecutionRequest();
        toolchains(context, request);
        populateRequest(context, context.lookup, request);
        return doExecute(context, request);
    }

    protected MavenExecutionRequest prepareMavenExecutionRequest() throws Exception {
        // explicitly fill in "defaults"?
        DefaultMavenExecutionRequest mavenExecutionRequest = new DefaultMavenExecutionRequest();
        mavenExecutionRequest.setRepositoryCache(new DefaultRepositoryCache());
        mavenExecutionRequest.setInteractiveMode(true);
        mavenExecutionRequest.setCacheTransferError(false);
        mavenExecutionRequest.setIgnoreInvalidArtifactDescriptor(true);
        mavenExecutionRequest.setIgnoreMissingArtifactDescriptor(true);
        mavenExecutionRequest.setRecursive(true);
        mavenExecutionRequest.setReactorFailureBehavior(MavenExecutionRequest.REACTOR_FAIL_FAST);
        mavenExecutionRequest.setStartInstant(MonotonicClock.now());
        mavenExecutionRequest.setLoggingLevel(MavenExecutionRequest.LOGGING_LEVEL_INFO);
        mavenExecutionRequest.setDegreeOfConcurrency(1);
        mavenExecutionRequest.setBuilderId("singlethreaded");
        return mavenExecutionRequest;
    }

    @Override
    protected void lookup(MavenContext context) throws Exception {
        if (context.maven == null) {
            super.lookup(context);
            context.maven = context.lookup.lookup(Maven.class);
        }
    }

    @Override
    protected void postCommands(MavenContext context) throws Exception {
        super.postCommands(context);

        InvokerRequest invokerRequest = context.invokerRequest;
        MavenOptions options = (MavenOptions) invokerRequest.options();
        Logger logger = context.logger;
        if (options.relaxedChecksums().orElse(false)) {
            logger.info("Disabling strict checksum verification on all artifact downloads.");
        } else if (options.strictChecksums().orElse(false)) {
            logger.info("Enabling strict checksum verification on all artifact downloads.");
        }
    }

    protected void toolchains(MavenContext context, MavenExecutionRequest request) throws Exception {
        Path userToolchainsFile = null;
        if (context.invokerRequest.options().altUserToolchains().isPresent()) {
            userToolchainsFile = context.cwdResolver.apply(
                    context.invokerRequest.options().altUserToolchains().get());

            if (!Files.isRegularFile(userToolchainsFile)) {
                throw new FileNotFoundException(
                        "The specified user toolchains file does not exist: " + userToolchainsFile);
            }
        } else {
            String userToolchainsFileStr =
                    context.protoSession.getUserProperties().get(Constants.MAVEN_USER_TOOLCHAINS);
            if (userToolchainsFileStr != null) {
                userToolchainsFile = context.cwdResolver.apply(userToolchainsFileStr);
            }
        }

        Path installationToolchainsFile = null;
        if (context.invokerRequest.options().altInstallationToolchains().isPresent()) {
            installationToolchainsFile = context.cwdResolver.apply(
                    context.invokerRequest.options().altInstallationToolchains().get());

            if (!Files.isRegularFile(installationToolchainsFile)) {
                throw new FileNotFoundException(
                        "The specified installation toolchains file does not exist: " + installationToolchainsFile);
            }
        } else {
            String installationToolchainsFileStr =
                    context.protoSession.getUserProperties().get(Constants.MAVEN_INSTALLATION_TOOLCHAINS);
            if (installationToolchainsFileStr != null) {
                installationToolchainsFile = context.cwdResolver.apply(installationToolchainsFileStr);
            }
        }

        request.setInstallationToolchainsFile(
                installationToolchainsFile != null ? installationToolchainsFile.toFile() : null);
        request.setUserToolchainsFile(userToolchainsFile != null ? userToolchainsFile.toFile() : null);

        ToolchainsBuilderRequest toolchainsRequest = ToolchainsBuilderRequest.builder()
                .session(context.protoSession)
                .installationToolchainsSource(
                        installationToolchainsFile != null && Files.isRegularFile(installationToolchainsFile)
                                ? PathSource.buildSource(installationToolchainsFile)
                                : null)
                .userToolchainsSource(
                        userToolchainsFile != null && Files.isRegularFile(userToolchainsFile)
                                ? PathSource.buildSource(userToolchainsFile)
                                : null)
                .build();

        context.eventSpyDispatcher.onEvent(toolchainsRequest);

        context.logger.debug("Reading installation toolchains from '" + installationToolchainsFile + "'");
        context.logger.debug("Reading user toolchains from '" + userToolchainsFile + "'");

        ToolchainsBuilderResult toolchainsResult =
                context.lookup.lookup(ToolchainsBuilder.class).build(toolchainsRequest);

        context.eventSpyDispatcher.onEvent(toolchainsResult);

        context.lookup
                .lookup(MavenExecutionRequestPopulator.class)
                .populateFromToolchains(
                        request,
                        new org.apache.maven.toolchain.model.PersistedToolchains(
                                toolchainsResult.getEffectiveToolchains()));

        if (toolchainsResult.getProblems().hasWarningProblems()) {
            int totalProblems = toolchainsResult.getProblems().totalProblemsReported();
            context.logger.info("");
            context.logger.info(String.format(
                    "%s %s encountered while building the effective toolchains (use -e to see details)",
                    totalProblems, (totalProblems == 1) ? "problem was" : "problems were"));

            if (context.invokerRequest.options().showErrors().orElse(false)) {
                for (BuilderProblem problem :
                        toolchainsResult.getProblems().problems().toList()) {
                    context.logger.warn(problem.getMessage() + " @ " + problem.getLocation());
                }
            }

            context.logger.info("");
        }
    }

    @Override
    protected void populateRequest(MavenContext context, Lookup lookup, MavenExecutionRequest request)
            throws Exception {
        super.populateRequest(context, lookup, request);
        if (context.invokerRequest.rootDirectory().isEmpty()) {
            // maven requires this to be set; so default it (and see below at POM)
            request.setMultiModuleProjectDirectory(
                    context.invokerRequest.topDirectory().toFile());
            request.setRootDirectory(context.invokerRequest.topDirectory());
        }

        MavenOptions options = (MavenOptions) context.invokerRequest.options();
        request.setNoSnapshotUpdates(options.suppressSnapshotUpdates().orElse(false));
        request.setGoals(options.goals().orElse(List.of()));
        request.setReactorFailureBehavior(determineReactorFailureBehaviour(context));
        request.setRecursive(!options.nonRecursive().orElse(!request.isRecursive()));
        request.setOffline(options.offline().orElse(request.isOffline()));
        request.setUpdateSnapshots(options.updateSnapshots().orElse(false));
        request.setGlobalChecksumPolicy(determineGlobalChecksumPolicy(context));

        Path pom = determinePom(context, lookup);
        if (pom != null) {
            request.setPom(pom.toFile());
            if (pom.getParent() != null) {
                request.setBaseDirectory(pom.getParent().toFile());
            }

            // project present, but we could not determine rootDirectory: extra work needed
            if (context.invokerRequest.rootDirectory().isEmpty()) {
                Path rootDirectory = Utils.findMandatoryRoot(context.invokerRequest.topDirectory());
                request.setMultiModuleProjectDirectory(rootDirectory.toFile());
                request.setRootDirectory(rootDirectory);
            }
        }

        request.setTransferListener(
                determineTransferListener(context, options.noTransferProgress().orElse(false)));
        request.setExecutionListener(determineExecutionListener(context));

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
        if (options.threads().isPresent()) {
            int degreeOfConcurrency =
                    calculateDegreeOfConcurrency(options.threads().get());
            if (degreeOfConcurrency > 1) {
                request.setBuilderId("multithreaded");
                request.setDegreeOfConcurrency(degreeOfConcurrency);
            }
        }

        //
        // Allow the builder to be overridden by the user if requested. The builders are now pluggable.
        //
        if (options.builder().isPresent()) {
            request.setBuilderId(options.builder().get());
        }
    }

    protected Path determinePom(MavenContext context, Lookup lookup) {
        Path current = context.invokerRequest.cwd();
        MavenOptions options = (MavenOptions) context.invokerRequest.options();
        if (options.alternatePomFile().isPresent()) {
            current = context.cwdResolver.apply(options.alternatePomFile().get());
        }
        ModelProcessor modelProcessor =
                lookup.lookupOptional(ModelProcessor.class).orElse(null);
        if (modelProcessor != null) {
            return modelProcessor.locateExistingPom(current);
        } else {
            return Files.isRegularFile(current) ? current : null;
        }
    }

    protected String determineReactorFailureBehaviour(MavenContext context) {
        MavenOptions mavenOptions = (MavenOptions) context.invokerRequest.options();
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

    protected String determineGlobalChecksumPolicy(MavenContext context) {
        MavenOptions mavenOptions = (MavenOptions) context.invokerRequest.options();
        if (mavenOptions.strictChecksums().orElse(false)) {
            return MavenExecutionRequest.CHECKSUM_POLICY_FAIL;
        } else if (mavenOptions.relaxedChecksums().orElse(false)) {
            return MavenExecutionRequest.CHECKSUM_POLICY_WARN;
        } else {
            return null;
        }
    }

    protected ExecutionListener determineExecutionListener(MavenContext context) {
        ExecutionListener listener = new ExecutionEventLogger(context.invokerRequest.messageBuilderFactory());
        if (context.eventSpyDispatcher != null) {
            listener = context.eventSpyDispatcher.chainListener(listener);
        }
        return new LoggingExecutionListener(listener, determineBuildEventListener(context));
    }

    protected TransferListener determineTransferListener(MavenContext context, boolean noTransferProgress) {
        boolean quiet = context.invokerRequest.options().quiet().orElse(false);
        boolean logFile = context.invokerRequest.options().logFile().isPresent();
        boolean runningOnCI = isRunningOnCI(context);
        boolean quietCI = runningOnCI
                && !context.invokerRequest.options().forceInteractive().orElse(false);

        TransferListener delegate;
        if (quiet || noTransferProgress || quietCI) {
            delegate = new QuietMavenTransferListener();
        } else if (context.interactive && !logFile) {
            delegate = new SimplexTransferListener(new ConsoleMavenTransferListener(
                    context.invokerRequest.messageBuilderFactory(),
                    context.terminal.writer(),
                    context.invokerRequest.options().verbose().orElse(false)));
        } else {
            delegate = new Slf4jMavenTransferListener();
        }
        return new MavenTransferListener(delegate, determineBuildEventListener(context));
    }

    protected String determineMakeBehavior(MavenContext context) {
        MavenOptions mavenOptions = (MavenOptions) context.invokerRequest.options();
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

    protected void performProjectActivation(MavenContext context, ProjectActivation projectActivation) {
        MavenOptions mavenOptions = (MavenOptions) context.invokerRequest.options();
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

    protected void performProfileActivation(MavenContext context, ProfileActivation profileActivation) {
        MavenOptions mavenOptions = (MavenOptions) context.invokerRequest.options();
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

    protected int doExecute(MavenContext context, MavenExecutionRequest request) throws Exception {
        context.eventSpyDispatcher.onEvent(request);

        MavenExecutionResult result;
        try {
            result = context.maven.execute(request);
            context.eventSpyDispatcher.onEvent(result);
        } finally {
            context.eventSpyDispatcher.close();
        }

        if (result.hasExceptions()) {
            ExceptionHandler handler = new DefaultExceptionHandler();
            Map<String, String> references = new LinkedHashMap<>();
            List<MavenProject> failedProjects = new ArrayList<>();

            for (Throwable exception : result.getExceptions()) {
                ExceptionSummary summary = handler.handleException(exception);
                logSummary(context, summary, references, "");

                if (exception instanceof LifecycleExecutionException lifecycleExecutionException) {
                    failedProjects.add(lifecycleExecutionException.getProject());
                }
            }

            context.logger.error("");

            if (!context.invokerRequest.options().showErrors().orElse(false)) {
                context.logger.error("To see the full stack trace of the errors, re-run Maven with the '"
                        + MessageUtils.builder().strong("-e") + "' switch");
            }
            if (!context.invokerRequest.options().verbose().orElse(false)) {
                context.logger.error("Re-run Maven using the '"
                        + MessageUtils.builder().strong("-X") + "' switch to enable verbose output");
            }

            if (!references.isEmpty()) {
                context.logger.error("");
                context.logger.error("For more information about the errors and possible solutions"
                        + ", please read the following articles:");

                for (Map.Entry<String, String> entry : references.entrySet()) {
                    context.logger.error(MessageUtils.builder().strong(entry.getValue()) + " " + entry.getKey());
                }
            }

            if (result.canResume()) {
                logBuildResumeHint(context, "mvn [args] -r");
            } else if (!failedProjects.isEmpty()) {
                List<MavenProject> sortedProjects = result.getTopologicallySortedProjects();

                // Sort the failedProjects list in the topologically sorted order.
                failedProjects.sort(comparing(sortedProjects::indexOf));

                MavenProject firstFailedProject = failedProjects.get(0);
                if (!firstFailedProject.equals(sortedProjects.get(0))) {
                    String resumeFromSelector = getResumeFromSelector(sortedProjects, firstFailedProject);
                    logBuildResumeHint(context, "mvn [args] -rf " + resumeFromSelector);
                }
            }

            if (((MavenOptions) context.invokerRequest.options()).failNever().orElse(false)) {
                context.logger.info("Build failures were ignored.");
                return 0;
            } else {
                return 1;
            }
        } else {
            return 0;
        }
    }

    protected void logBuildResumeHint(MavenContext context, String resumeBuildHint) {
        context.logger.error("");
        context.logger.error("After correcting the problems, you can resume the build with the command");
        context.logger.error(
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

    protected void logSummary(
            MavenContext context, ExceptionSummary summary, Map<String, String> references, String indent) {
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
                    && (context.invokerRequest.options().showErrors().orElse(false)
                            || (summary.getException() instanceof InternalErrorException))) {
                context.logger.error(line, summary.getException());
            } else {
                context.logger.error(line);
            }

            currentColor = nextColor;
        }

        indent += "  ";

        for (ExceptionSummary child : summary.getChildren()) {
            logSummary(context, child, references, indent);
        }
    }
}
