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

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;

import org.apache.maven.api.Session;
import org.apache.maven.api.annotations.Nullable;
import org.apache.maven.api.cli.InvokerException;
import org.apache.maven.api.cli.InvokerRequest;
import org.apache.maven.api.cli.mvnval.ValidateOptions;
import org.apache.maven.api.services.Lookup;
import org.apache.maven.api.services.ModelBuilder;
import org.apache.maven.api.services.ModelBuilder.ModelBuilderSession;
import org.apache.maven.api.services.ModelBuilderException;
import org.apache.maven.api.services.ModelBuilderRequest;
import org.apache.maven.api.services.ModelBuilderResult;
import org.apache.maven.api.services.ModelProblem;
import org.apache.maven.api.services.Sources;
import org.apache.maven.cling.invoker.LookupContext;
import org.apache.maven.cling.invoker.LookupInvoker;
import org.apache.maven.impl.standalone.ApiRunner;
import org.jline.reader.UserInterruptException;
import org.jline.terminal.Terminal;

/**
 * Validates POM files without building them.
 * <p>
 * Validation stops after {@link ModelBuilder.ModelBuilderSession#validate}, which reads no
 * further than the raw model, so no parent is resolved and the verdict comes from the POM files
 * themselves. The problems it can reach are therefore a subset: a dependency whose version comes
 * from the parent's {@code dependencyManagement} is only checked later, in
 * {@code validateEffectiveModel}.
 * <p>
 * It stands up no container and reads no settings, so a directory holding a POM cannot make this
 * process fetch or run anything. What it cannot refuse is the JVM: {@code bin/mvn} hands
 * {@code .mvn/jvm.config} to the launcher before any of this runs.
 */
public class ValidateInvoker extends LookupInvoker<ValidateContext> {

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
        try {
            context.terminal.handle(
                    Terminal.Signal.INT, signal -> Thread.currentThread().interrupt());

            OutputFormat format;
            try {
                // The parser checks --format too, but options are interpolated afterwards, so
                // ${...} can still become anything by the time it reaches here.
                format = context.options().format().map(OutputFormat::parse).orElse(OutputFormat.TEXT);
            } catch (IllegalArgumentException e) {
                context.logger.error(e.getMessage());
                return BAD_OPERATION;
            }

            List<Report> reports = validateAll(resolvePoms(context), createSession());
            // interrupted(), not isInterrupted(): clear the flag where it is acted on. Reporting
            // here would give a verdict on only the files reached so far.
            if (Thread.interrupted()) {
                return canceled(context);
            }
            // determineWriter, not context.writer: on a real run that field is still empty and
            // this is what fills it.
            format.report(reports, determineWriter(context));
            return exitCode(reports);
        } catch (UserInterruptException e) {
            return canceled(context);
        } catch (Exception e) {
            // Without this an unexpected failure escapes to the CLI and becomes 2, which this tool
            // documents as bad usage. Both siblings catch here too.
            if (context.options().showErrors().orElse(false)) {
                context.logger.error(e.getMessage(), e);
            } else {
                context.logger.error(e.getMessage());
            }
            return ERROR;
        }
    }

    private static int canceled(ValidateContext context) {
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

    /** No settings: nothing here resolves, mirrors, proxies or authenticates. */
    @Override
    protected void settings(ValidateContext context) {}

    /**
     * Creates the session the model builder runs on. Made here because {@code LookupInvoker} only
     * ever builds a {@code ProtoSession} while {@link ModelBuilderRequest} needs a full
     * {@link Session}; a standalone one suffices, as nothing is resolved.
     *
     * @return the session, never {@code null}
     */
    protected Session createSession() {
        return ApiRunner.createSession();
    }

    private static List<Path> resolvePoms(ValidateContext context) {
        List<String> args = context.options().poms().orElse(List.of());
        return args.isEmpty()
                ? List.of(context.cwd.resolve("pom.xml"))
                : args.stream().map(context.cwd::resolve).distinct().toList();
    }

    private static List<Report> validateAll(List<Path> poms, Session session) {
        // One session for the run, not per file. It saves no reading: every validate() walks
        // the reactor from the root again.
        ModelBuilderSession builderSession =
                session.getService(ModelBuilder.class).newSession();
        List<Report> reports = new ArrayList<>(poms.size());
        for (Path pom : poms) {
            // Ctrl+C lands between files, not part way through one.
            if (Thread.currentThread().isInterrupted()) {
                break;
            }
            reports.add(validateOne(pom, session, builderSession));
        }
        return reports;
    }

    private static Report validateOne(Path pom, Session session, ModelBuilderSession builderSession) {
        String unusable = unusable(pom);
        if (unusable != null) {
            return Report.failed(pom, unusable);
        }
        ModelBuilderRequest request = ModelBuilderRequest.builder()
                .session(session)
                .source(Sources.buildSource(pom))
                .requestType(ModelBuilderRequest.RequestType.BUILD_PROJECT)
                .build();
        try {
            return Report.of(pom, problemsOf(builderSession.validate(request)));
        } catch (ModelBuilderException e) {
            // A result that explains nothing would report the file as clean, so use the exception.
            ModelBuilderResult result = e.getResult();
            return result != null && result.getProblemCollector().hasErrorProblems()
                    ? Report.of(pom, problemsOf(result))
                    : Report.failed(pom, describe(e));
        } catch (UnsupportedOperationException e) {
            // validate is unimplemented: the tool is unusable, rather than this file being bad.
            throw e;
        } catch (Exception e) {
            // One bad file must not lose the verdict on the others.
            return Report.failed(pom, describe(e));
        }
    }

    private static List<ModelProblem> problemsOf(ModelBuilderResult result) {
        return result.getProblemCollector().problems().toList();
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
