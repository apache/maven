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
package org.apache.maven.cling.invoker.mvnval;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.Consumer;
import java.util.stream.Stream;

import org.apache.maven.api.Constants;
import org.apache.maven.api.RemoteRepository;
import org.apache.maven.api.Session;
import org.apache.maven.api.annotations.Nullable;
import org.apache.maven.api.cli.InvokerException;
import org.apache.maven.api.cli.InvokerRequest;
import org.apache.maven.api.cli.mvnval.ValidateOptions;
import org.apache.maven.api.di.Named;
import org.apache.maven.api.di.Provides;
import org.apache.maven.api.model.Model;
import org.apache.maven.api.model.Profile;
import org.apache.maven.api.model.Repository;
import org.apache.maven.api.model.RepositoryPolicy;
import org.apache.maven.api.services.Lookup;
import org.apache.maven.api.services.ModelBuilder;
import org.apache.maven.api.services.ModelBuilder.ModelBuilderSession;
import org.apache.maven.api.services.ModelBuilderException;
import org.apache.maven.api.services.ModelBuilderRequest;
import org.apache.maven.api.services.ModelBuilderRequest.RequestType;
import org.apache.maven.api.services.ModelBuilderResult;
import org.apache.maven.api.services.RepositoryFactory;
import org.apache.maven.api.services.SettingsBuilder;
import org.apache.maven.api.services.Sources;
import org.apache.maven.cling.invoker.LookupContext;
import org.apache.maven.cling.invoker.LookupInvoker;
import org.apache.maven.cling.logging.Slf4jConfiguration;
import org.apache.maven.impl.InternalSession;
import org.apache.maven.impl.standalone.ApiRunner;
import org.eclipse.aether.DefaultRepositorySystemSession;
import org.eclipse.aether.spi.connector.transport.TransporterFactory;
import org.eclipse.aether.spi.connector.transport.http.ChecksumExtractor;
import org.eclipse.aether.spi.io.PathProcessor;
import org.eclipse.aether.transport.apache.ApacheTransporterFactory;
import org.eclipse.aether.transport.file.FileTransporterFactory;
import org.jline.reader.UserInterruptException;
import org.jline.terminal.Terminal;

/**
 * Validates POM files without building them.
 * <p>
 * Two modes, and one contains the other. {@code raw} stops at
 * {@link ModelBuilder.ModelBuilderSession#validate}, reaching no repository at all: it works
 * offline and on a POM whose parent is not published yet, but it cannot see a problem that
 * inheritance introduces, such as a dependency whose version comes from the parent's
 * {@code dependencyManagement}. {@code effective}, the default, runs that pass and then builds
 * the effective model as well, so parents and imported boms are resolved and the checks needing
 * them run too. Both passes, because the effective build says nothing about the reactor around a
 * POM: on its own it passes a project whose declared subproject is not on disk, which
 * {@code mvn} refuses to read.
 * <p>
 * Nothing is ever written back to a POM. In {@code effective} mode what is resolved lands in the
 * local repository, as it does for every Maven tool. {@code --local-repository} aims that
 * somewhere else and {@code --temp-local-repository} at a directory made for the run and deleted
 * when it ends, for a gate that wants the configured repository left as it was.
 * <p>
 * It stands up no container, so the directory holding a POM cannot make this process load
 * anything through {@code .mvn/extensions.xml}, {@code maven.ext.class.path} or
 * {@code .mvn/settings.xml}. The user's own settings are another matter: resolution reads them
 * for mirrors, proxies and credentials, as it has to. The JVM is outside this:
 * {@code bin/mvn} passes {@code .mvn/jvm.config} to the launcher before this process starts.
 * <p>
 * {@code -o} works in {@code effective} mode: resolution hits the local repository only and
 * fails fast when a parent or BOM is absent. Combined with {@code --temp-local-repository} it
 * proves that the POMs being validated are fully self-contained.
 * <p>
 * When more than one POM is given, {@code effective} mode pre-scans them all to build a bundle
 * workspace: a parent that is named in the bundle is resolved from disk rather than from a
 * repository, so the verdict does not depend on publication order and the run does not download
 * what is already on disk.
 */
public class ValidateInvoker extends LookupInvoker<ValidateContext> {

    /** Where Central lives, for the one case where no settings file names a repository. */
    private static final String CENTRAL_URL = "https://repo.maven.apache.org/maven2";

    public static final int OK = 0;
    public static final int ERROR = 1;

    /** Bad user input. Same meaning as in {@code mvnenc} and {@code mvnup}. */
    public static final int BAD_OPERATION = 2;

    /**
     * Interrupted. Same meaning as in {@code mvnenc} and {@code mvnup}, and reachable only with a
     * terminal attached: without one the signal reaches the JVM instead and the exit code is its.
     * Left out of {@code --help} for that reason.
     */
    public static final int CANCELED = 3;

    /**
     * Nothing was rejected, but something was reported. Numbered 4 rather than reusing a lower
     * code: 0 to 3 mean the same thing across the Maven 4 tools, and a gate must be able to tell
     * "this POM has warnings" from "mvnval broke" or "somebody hit Ctrl+C".
     */
    public static final int WARNINGS = 4;

    public ValidateInvoker(Lookup protoLookup, @Nullable Consumer<LookupContext> contextConsumer) {
        super(protoLookup, contextConsumer);
    }

    @Override
    protected ValidateContext createContext(InvokerRequest invokerRequest) {
        return new ValidateContext(
                invokerRequest, (ValidateOptions) invokerRequest.options().orElse(null));
    }

    @Override
    protected int execute(ValidateContext context) throws Exception {
        Thread validationThread = Thread.currentThread();
        AtomicBoolean cancellationRequested = new AtomicBoolean();
        Terminal.SignalHandler previousHandler = context.terminal.handle(Terminal.Signal.INT, signal -> {
            cancellationRequested.set(true);
            validationThread.interrupt();
        });
        try {
            OutputFormat format;
            ValidationMode mode;
            try {
                // Checked here rather than while parsing: the parser knows the option names,
                // not the values, and a value it did accept can still become anything once
                // options are interpolated.
                format = context.options().format().map(OutputFormat::parse).orElse(OutputFormat.TEXT);
                mode = context.options().mode().map(ValidationMode::parse).orElse(ValidationMode.EFFECTIVE);
            } catch (IllegalArgumentException e) {
                context.logger.error(e.getMessage());
                return BAD_OPERATION;
            }
            // -s/-ps/-is redirect the settings files that ApiRunner reads at session-creation time.
            // Honouring them would require threading the alternate path into ApiRunner before it
            // starts; that is a larger change than belongs here, so they are still refused.
            // -o is different: it applies to the resolver session, which createSession() returns,
            // and can be applied there after the session is built.  Raw mode resolves nothing, so
            // none of these apply there.
            if (mode != ValidationMode.RAW) {
                String refused = refusedSettingsOption(context.options());
                if (refused != null) {
                    context.logger.error(refused
                            + " cannot redirect the settings file mvnval reads."
                            + " Use the default settings, or copy the relevant settings to the default location.");
                    return BAD_OPERATION;
                }
            }

            if (context.options().localRepository().isPresent()
                    && context.options().tempLocalRepository().orElse(false)) {
                context.logger.error(
                        "--local-repository and --temp-local-repository name different directories; give one.");
                return BAD_OPERATION;
            }
            String unusableRepository = unusableRepository(context);
            if (unusableRepository != null) {
                context.logger.error(unusableRepository);
                return BAD_OPERATION;
            }
            Path localRepository = null;
            // Only in effective mode: raw resolves nothing, so making a directory there could
            // lose a run that would never have written to it.
            if (mode != ValidationMode.RAW) {
                try {
                    localRepository = localRepository(context);
                } catch (IOException e) {
                    // Not ERROR: no POM was rejected, the run could not be set up.
                    context.logger.error("Could not create a temporary local repository: " + e);
                    return BAD_OPERATION;
                }
            }
            Thread cleanup = temporary(context, localRepository) ? cleanupHook(localRepository, context) : null;
            try {
                List<Report> reports =
                        validateAll(context, createSession(localRepository), mode, cancellationRequested);
                if (cancellationRequested.get() || Thread.currentThread().isInterrupted()) {
                    return canceled(context);
                }
                // determineWriter, not context.writer: on a real run that field is still empty
                // and this is what fills it.
                int contextLines = context.options().context().orElse(2);
                format.report(
                        reports,
                        context.cwd.get(),
                        determineWriter(context),
                        contextLines,
                        Boolean.TRUE.equals(context.coloredOutput));
                return exitCode(reports);
            } finally {
                if (cleanup != null) {
                    // Delete first, deregister second. The other order leaves a window where a
                    // signal arriving after the hook is gone kills the process before the
                    // deletion runs.
                    deleteRecursively(localRepository, context);
                    removeHook(cleanup);
                }
            }
        } catch (UnsupportedOperationException e) {
            // The ModelBuilder in this container has not written validate. Nothing was wrong with
            // any POM, so this must not be ERROR, the code a gate reads as "a POM was rejected".
            context.logger.error("This Maven installation cannot validate POMs: " + e.getMessage());
            return BAD_OPERATION;
        } catch (UserInterruptException e) {
            return canceled(context);
        } catch (Exception e) {
            if (cancellationRequested.get() || Thread.currentThread().isInterrupted()) {
                return canceled(context);
            }
            // Without this an unexpected failure escapes to the CLI and becomes 2, which this tool
            // documents as bad usage. Both siblings catch here too.
            if (context.options().showErrors().orElse(false)) {
                context.logger.error(e.getMessage(), e);
            } else {
                context.logger.error(e.getMessage());
            }
            return ERROR;
        } finally {
            context.terminal.handle(Terminal.Signal.INT, previousHandler);
            if (cancellationRequested.get()) {
                Thread.interrupted();
            }
        }
    }

    private static int canceled(ValidateContext context) {
        Thread.interrupted();
        context.logger.error("Validation canceled by user.");
        return CANCELED;
    }

    /**
     * Turns an unparseable command line into {@link #BAD_OPERATION}.
     * <p>
     * The base exits 1 for a bad argument, which is the code this tool uses for a POM it rejected.
     * A gate has to be able to tell a typo in its own script from a POM that failed validation.
     */
    @Override
    protected void validate(ValidateContext context) throws Exception {
        try {
            super.validate(context);
        } catch (InvokerException.ExitException e) {
            throw e.getExitCode() == ERROR ? new InvokerException.ExitException(BAD_OPERATION) : e;
        }
    }

    /** Whether stdout carries a document this run must not write anything else to. */
    private static boolean json(ValidateContext context) {
        return context.options()
                .format()
                .filter(OutputFormat.JSON.name()::equalsIgnoreCase)
                .isPresent();
    }

    /**
     * Keeps the version banner out of the document under {@code --format json}.
     * <p>
     * The base prints it whenever {@code effectiveVerbose()} holds, which a CI runner setting
     * {@code RUNNER_DEBUG=1} makes true, so re-running a job with debug logging turned the
     * document into something no parser accepts. Asking for {@code -V} still prints it, since
     * that is what the flag is for.
     */
    @Override
    protected void preCommands(ValidateContext context) throws Exception {
        if (json(context) && !context.options().showVersion().orElse(false)) {
            return;
        }
        super.preCommands(context);
    }

    /**
     * Suppresses resolver noise from the output unless the caller asked for it.
     * <p>
     * The resolver writes an {@code [INFO]} line the first time it reads a repository's prefix
     * file. Under {@code --format json} that line breaks the document; in text mode it clutters
     * output that is meant to list only POM problems. Neither case benefits from seeing it.
     * The level is forced to {@code ERROR} — the same effect as {@code -q} — unless {@code -X}
     * or {@code -e} is given, in which case the full log is what the caller wants.
     * <p>
     * The property must be set <em>before</em> calling {@link Slf4jConfiguration#setRootLoggerLevel}
     * because that method itself logs at {@code INFO} when it overrides a value already set (e.g.
     * a CI runner running with {@code DEBUG}), which would be the very noise we are suppressing.
     */
    @Override
    protected void configureLogging(ValidateContext context) throws Exception {
        super.configureLogging(context);
        if (!context.options().verbose().orElse(false)
                && !context.options().showErrors().orElse(false)) {
            context.loggerLevel = Slf4jConfiguration.Level.ERROR;
            System.setProperty(Constants.MAVEN_LOGGER_DEFAULT_LOG_LEVEL, "error");
            context.slf4jConfiguration.setRootLoggerLevel(context.loggerLevel);
        }
    }

    // The next five steps stand up dependency injection and read configuration. Nothing here uses
    // either: the model builder comes from createSession(). Running them would let the directory
    // holding the POM decide what this process loads, via .mvn/extensions.xml, .mvn/settings.xml,
    // or maven.ext.class.path in .mvn/maven-user.properties. Refused one at a time rather than by
    // replacing doInvoke, so a step added to the base class later still runs.

    /** No container: nothing is looked up, and no extension or contributed property is loaded. */
    @Override
    protected void container(ValidateContext context) {}

    /** No container, so no {@code PropertyContributor} to run. */
    @Override
    protected void postContainer(ValidateContext context) {}

    /** No container to look up from. */
    @Override
    protected void lookup(ValidateContext context) {}

    /** No {@code EventSpy} dispatch: this tool emits no build events. */
    @Override
    protected void init(ValidateContext context) {}

    /**
     * No settings step: the model builder runs on the session from {@link #createSession(Path)},
     * which reads the user's settings itself when it has to resolve.
     */
    @Override
    protected void settings(ValidateContext context) {}

    /**
     * Creates the session the model builder runs on. Made here because {@code LookupInvoker} only
     * ever builds a {@code ProtoSession} while {@link ModelBuilderRequest} needs a full
     * {@link Session}. Left {@code protected} as a seam: building one is the expensive part of
     * every test in this package, and the tests substitute a shared one.
     *
     * @return the session, never {@code null}
     */
    protected Session createSession(@Nullable Path localRepository) {
        Session session = ApiRunner.createSession(injector -> injector.bindImplicit(TransporterFactoryConfig.class));
        if (localRepository != null) {
            // Set on the session that came back, not passed to createSession, which prefers
            // settings.getLocalRepository() over its argument and may in any case hand back a
            // session another tool built. See derivedRepositories for why that happens.
            session = session.withLocalRepository(session.createLocalRepository(localRepository));
        }
        return session.withRemoteRepositories(derivedRepositories(session));
    }

    /**
     * Applies {@code --offline} to a session built by {@link #createSession}.
     * <p>
     * {@code ApiRunner.createSession} applies offline from the settings file.  This supplements
     * that: when the caller passes {@code -o} on the command line, the session is made offline
     * regardless of what the settings say, by cloning the underlying resolver session with offline
     * set to true.
     */
    private static Session withOffline(Session session) {
        // withLocalRepository(same repo) forces AbstractSession to clone the underlying resolver
        // session (DefaultRepositorySystemSession) into a fresh one held only by the new session
        // object.  That fresh clone is safe to mutate without affecting the caller's session.
        Session copy = session.withLocalRepository(session.getLocalRepository());
        DefaultRepositorySystemSession rsession =
                (DefaultRepositorySystemSession) InternalSession.from(copy).getSession();
        rsession.setOffline(true);
        return copy;
    }

    /**
     * Installs a {@link BundleWorkspaceReader} on the resolver session so that parent and BOM
     * lookups for POMs in the bundle are answered from disk rather than from a repository.
     * <p>
     * The workspace reader is set on a clone of the resolver session: it does not mutate the
     * original session, which the tests may reuse across runs.
     */
    private static Session withWorkspaceReader(Session session, BundleWorkspaceReader reader) {
        Session copy = session.withLocalRepository(session.getLocalRepository());
        DefaultRepositorySystemSession rsession =
                (DefaultRepositorySystemSession) InternalSession.from(copy).getSession();
        rsession.setWorkspaceReader(reader);
        return copy;
    }

    /**
     * The repositories to resolve from, derived here rather than taken as they came.
     * <p>
     * {@code ApiRunner.createSession} asks its injector for an unqualified {@code Session}, and
     * {@code InjectorImpl} registers a qualified {@code @Provides} under the unqualified key as
     * well, so a provider belonging to a sibling tool can answer it and bring its own hardcoded
     * repositories. Deriving the list from the settings gives the same answer whichever session
     * came back, and keeps the repositories an operator declared: a parent that lives only in an
     * internal repository has to resolve here exactly as it does under {@code mvn}.
     * <p>
     * The fallback matters only where no settings file declares a repository, which for a real
     * installation means never: {@code conf/settings.xml} ships Central. It states Central with
     * snapshots off, as that file does, because the two-argument
     * {@link Session#createRemoteRepository(String, String)} takes the resolver's defaults
     * instead, which enable snapshots and lower checksum checking to a warning.
     */
    private static List<RemoteRepository> derivedRepositories(Session session) {
        RepositoryFactory factory = session.getService(RepositoryFactory.class);
        Profile settingsProfile = session.getService(SettingsBuilder.class)
                .convert(org.apache.maven.api.settings.Profile.newBuilder()
                        .repositories(session.getSettings().getRepositories())
                        .build());
        List<RemoteRepository> repositories = settingsProfile.getRepositories().stream()
                .map(factory::createRemote)
                .toList();
        return repositories.isEmpty()
                ? List.of(factory.createRemote(Repository.newBuilder()
                        .id(RemoteRepository.CENTRAL_ID)
                        .url(CENTRAL_URL)
                        .releases(RepositoryPolicy.newBuilder().enabled("true").build())
                        .snapshots(
                                RepositoryPolicy.newBuilder().enabled("false").build())
                        .build()))
                : repositories;
    }

    /**
     * The transports resolution needs. {@code ApiRunner.createSession} registers none of its own,
     * so a session built without these reports "No transporter factories registered" and cannot
     * fetch from anywhere. {@code mvnup} carries the same block for the same reason.
     */
    static class TransporterFactoryConfig {
        @Provides
        @Named(ApacheTransporterFactory.NAME)
        static TransporterFactory apacheTransporterFactory(
                ChecksumExtractor checksumExtractor, PathProcessor pathProcessor) {
            return new ApacheTransporterFactory(checksumExtractor, pathProcessor);
        }

        @Provides
        @Named(FileTransporterFactory.NAME)
        static TransporterFactory fileTransporterFactory() {
            return new FileTransporterFactory();
        }
    }

    /** Whether {@code localRepository} is the throwaway directory this run made. */
    private static boolean temporary(ValidateContext context, @Nullable Path localRepository) {
        return localRepository != null
                && context.options().tempLocalRepository().orElse(false);
    }

    /**
     * Deletes the throwaway directory when the process is signalled.
     * <p>
     * The {@code finally} covers every way out of {@code execute}, but not a signal, and a CI job
     * killed on timeout is the case this option exists for. Without the hook a {@code SIGTERM} part way
     * through a run left the populated directory behind. {@code SIGKILL} still escapes, as it escapes every hook.
     */
    private static Thread cleanupHook(Path directory, ValidateContext context) {
        Thread hook = new Thread(() -> deleteRecursively(directory, context), "mvnval-repo-cleanup");
        Runtime.getRuntime().addShutdownHook(hook);
        return hook;
    }

    /** Takes the hook off, unless the shutdown it guards against has already begun. */
    private static void removeHook(Thread hook) {
        try {
            Runtime.getRuntime().removeShutdownHook(hook);
        } catch (IllegalStateException e) {
            // Shutdown is under way and the hook is running or about to; it does the same work.
        }
    }

    /**
     * The local repository to resolve into, or {@code null} to leave the configured one alone.
     * The throwaway directory is made here rather than in {@link #createSession(Path)} so the
     * caller owns it and can delete it on every path out.
     */
    @Nullable
    private static Path localRepository(ValidateContext context) throws IOException {
        if (context.options().tempLocalRepository().orElse(false)) {
            return Files.createTempDirectory("mvnval-repo-");
        }
        // maven.repo.local is how every other Maven command aims the local repository, so
        // swallowing it while offering --local-repository would be the trap this tool refuses
        // -o and -s to avoid. The option wins, being the more specific request.
        return context.options()
                .localRepository()
                .or(() -> Optional.ofNullable(
                        context.protoSession.getEffectiveProperties().get(Constants.MAVEN_REPO_LOCAL)))
                .map(context.cwd::resolve)
                .orElse(null);
    }

    /**
     * Deletes the temporary local repository, deepest entry first. A failure here is reported and
     * not thrown: the verdict on the POMs is already given, and losing it over a leftover
     * directory would be the wrong trade. A process killed before this runs leaves the directory
     * behind, where the operating system's temporary cleanup finds it.
     */
    private static void deleteRecursively(Path directory, ValidateContext context) {
        if (!Files.exists(directory)) {
            return;
        }
        try (Stream<Path> entries = Files.walk(directory)) {
            for (Path entry : entries.sorted(Comparator.reverseOrder()).toList()) {
                Files.deleteIfExists(entry);
            }
        } catch (IOException e) {
            context.logger.warn("Could not delete the temporary local repository " + directory + ": " + e);
        }
    }

    /**
     * Names the first settings-redirect option given that this tool cannot act on, or
     * {@code null} when none was given.
     * <p>
     * {@code -o} is intentionally absent: it is honoured in {@link #validateAll}.
     */
    @Nullable
    private static String refusedSettingsOption(ValidateOptions options) {
        if (options.altUserSettings().isPresent()) {
            return "--settings (-s)";
        }
        if (options.altProjectSettings().isPresent()) {
            return "--project-settings (-ps)";
        }
        if (options.altInstallationSettings().isPresent()) {
            // Both spellings, because the parser folds -gs into this one and naming only the
            // long form tells someone who typed -gs about an option they did not use.
            return "--install-settings (-is, -gs)";
        }
        return null;
    }

    private static List<Path> resolvePoms(ValidateContext context) {
        List<String> args = context.options().poms().orElse(List.of());
        return args.isEmpty()
                ? List.of(context.cwd.resolve("pom.xml"))
                : args.stream()
                        .map(context.cwd::resolve)
                        .map(ValidateInvoker::sameFileSamePath)
                        .distinct()
                        .toList();
    }

    private static List<Report> validateAll(
            ValidateContext context, Session session, ValidationMode mode, AtomicBoolean cancellationRequested) {
        // Apply --offline after session creation: the session already has mirrors, proxies and
        // credentials from the settings; making it offline only prevents the resolver from
        // opening connections, which is exactly what -o requests.
        if (context.options().offline().orElse(false)) {
            session = withOffline(session);
        }

        List<Path> poms = resolvePoms(context);
        ModelBuilder builder = session.getService(ModelBuilder.class);

        // In effective mode with more than one POM, build a bundle workspace so that parent and
        // BOM lookups for members of the bundle are answered from disk.  This is safe to share
        // across sessions because the index is immutable: it is built once here and never written
        // to.  The ordering bug that forced per-POM sessions arose from mappedSources, which is
        // mutable session state; the workspace reader has no such shared mutable state.
        if (mode == ValidationMode.EFFECTIVE && poms.size() > 1) {
            BundleWorkspaceReader bundleReader = buildBundleWorkspace(poms, session, builder);
            session = withWorkspaceReader(session, bundleReader);
        }

        final Session resolvedSession = session;
        List<Report> reports = new ArrayList<>(poms.size());
        for (Path pom : poms) {
            if (cancellationRequested.get() || Thread.currentThread().isInterrupted()) {
                break;
            }
            ModelBuilderRequest request = ModelBuilderRequest.builder()
                    .session(resolvedSession)
                    .source(Sources.buildSource(pom))
                    .requestType(RequestType.BUILD_PROJECT)
                    .userProperties(context.protoSession.getUserProperties())
                    .build();
            // A session per POM. Sharing one made the verdict depend on argument order: derived
            // sessions share mappedSources, so a groupId:artifactId registered while reading an
            // earlier POM answered a later POM's parent lookup, and two files given in the other
            // order came back with different problems. Sharing bought no reading either, since
            // every pass walks the reactor from the root again.
            reports.add(validateOne(request, builder.newSession(), mode));
        }
        return reports;
    }

    /**
     * Pre-scans all input POMs to build the bundle workspace index.
     * <p>
     * The index maps {@code groupId:artifactId} to the POM file on disk.  Scanning uses
     * {@link ModelBuilder#buildRawModel} so that only the file is read and no parent resolution
     * runs.  A POM that cannot be parsed is skipped: the validation pass that follows will report
     * it as broken, which is the right place for that verdict.
     * <p>
     * The index is keyed by GA rather than GAV because a bundle is expected to carry at most one
     * version of any given module, and a version mismatch between a child's declaration and the
     * parent found in the bundle is something the validator should report rather than a reason to
     * fall through to the repository.
     */
    private static BundleWorkspaceReader buildBundleWorkspace(List<Path> poms, Session session, ModelBuilder builder) {
        Map<String, File> index = new HashMap<>();
        Map<String, Model> models = new HashMap<>();
        for (Path pom : poms) {
            if (!Files.isRegularFile(pom) || !Files.isReadable(pom)) {
                continue;
            }
            try {
                ModelBuilderRequest req = ModelBuilderRequest.builder()
                        .session(session)
                        .source(Sources.buildSource(pom))
                        .requestType(RequestType.BUILD_PROJECT)
                        .build();
                Model model = builder.buildRawModel(req);
                String groupId = model.getGroupId() != null
                        ? model.getGroupId()
                        : (model.getParent() != null ? model.getParent().getGroupId() : null);
                String artifactId = model.getArtifactId();
                if (groupId != null && artifactId != null) {
                    String ga = groupId + ":" + artifactId;
                    index.put(ga, pom.toFile());
                    models.put(ga, model);
                }
            } catch (Exception e) {
                // Validation will report this POM as broken; skip it for the workspace index.
            }
        }
        return new BundleWorkspaceReader(index, models);
    }

    private static Report validateOne(
            ModelBuilderRequest request, ModelBuilderSession builderSession, ValidationMode mode) {
        Path pom = request.getSource().getPath();
        String unusable = unusable(pom);
        if (unusable != null) {
            return Report.failed(pom, unusable);
        }
        try {
            return Report.of(pom, mode.problemsFor(builderSession, request));
        } catch (ModelBuilderException e) {
            // A result that explains nothing would report the file as clean, so use the exception.
            ModelBuilderResult result = e.getResult();
            return result != null && result.getProblemCollector().hasErrorProblems()
                    ? Report.of(pom, ValidationMode.problemsOf(result))
                    : Report.failed(pom, describe(e));
        } catch (UnsupportedOperationException e) {
            // Past the catch-all below, or it would be reported as this file being bad. execute()
            // turns it into BAD_OPERATION: the tool cannot run at all here.
            throw e;
        } catch (Exception e) {
            // One bad file must not lose the verdict on the others.
            return Report.failed(pom, describe(e));
        }
    }

    /**
     * Says why {@code --local-repository} cannot be used, or {@code null} when it can. A path that
     * exists and is not a directory reaches the resolver and comes back as a ClassCastException
     * between two exception types, reported against the POM as though the POM were at fault.
     */
    @Nullable
    private static String unusableRepository(ValidateContext context) {
        return context.options()
                .localRepository()
                .map(context.cwd::resolve)
                .filter(Files::exists)
                .filter(path -> !Files.isDirectory(path))
                .map(path -> "--local-repository " + path + " is not a directory.")
                .orElse(null);
    }

    /**
     * The one spelling of a path, so that naming a file twice reports it once. {@code "pom.xml"}
     * and {@code "./pom.xml"} differ before normalising, and on Windows or macOS {@code POM.XML}
     * is the same file again. {@link Path#toRealPath} settles all of those, including a symbolic
     * link, and needs the file to be there; when it is not, the normalised path goes through and
     * {@code unusable} reports it missing.
     */
    private static Path sameFileSamePath(Path pom) {
        try {
            return pom.toRealPath();
        } catch (IOException e) {
            return pom.normalize();
        }
    }

    /**
     * Says why the file cannot be validated, or {@code null} when it can. A directory gets its own
     * message because {@code mvn -f} accepts one and looks for a {@code pom.xml} inside, so it is
     * the mistake most likely to be made here.
     */
    @Nullable
    private static String unusable(Path pom) {
        if (Files.isDirectory(pom)) {
            return "is a directory, not a POM file";
        }
        if (!Files.exists(pom)) {
            return "does not exist";
        }
        if (!Files.isRegularFile(pom)) {
            return "not a regular file";
        }
        return Files.isReadable(pom) ? null : "not readable";
    }

    /** Falls back to the class name rather than printing {@code null} for a message-less one. */
    private static String describe(Exception e) {
        String message = e.getMessage();
        return message != null && !message.isEmpty() ? message : e.toString();
    }

    private static int exitCode(List<Report> reports) {
        if (reports.stream().anyMatch(Report::hasErrors)) {
            return ERROR;
        }
        return reports.stream().allMatch(Report::clean) ? OK : WARNINGS;
    }
}
