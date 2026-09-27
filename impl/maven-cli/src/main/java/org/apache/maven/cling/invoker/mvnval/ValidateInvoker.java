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
import org.apache.maven.api.cli.InvokerRequest;
import org.apache.maven.api.cli.mvnval.ValidateOptions;
import org.apache.maven.api.services.Lookup;
import org.apache.maven.api.services.ModelBuilder;
import org.apache.maven.api.services.ModelBuilderException;
import org.apache.maven.api.services.ModelBuilderRequest;
import org.apache.maven.api.services.ModelBuilderResult;
import org.apache.maven.api.services.ModelProblem;
import org.apache.maven.api.services.Sources;
import org.apache.maven.cling.invoker.CoreExtensionSelector;
import org.apache.maven.cling.invoker.LookupContext;
import org.apache.maven.cling.invoker.LookupInvoker;
import org.apache.maven.impl.standalone.ApiRunner;

/**
 * Validates POM files without building them.
 * <p>
 * Validation stops after {@link ModelBuilder.ModelBuilderSession#buildRaw}, so no parent is
 * resolved and the verdict depends on the files alone, never on the network. The problems it can
 * reach are therefore a subset: a dependency whose version comes from the parent's
 * {@code dependencyManagement} is only checked later, in {@code validateEffectiveModel}.
 */
public class ValidateInvoker extends LookupInvoker<ValidateContext> {

    public static final int OK = 0;
    public static final int ERROR = 1;

    /** Bad user input, as in {@code mvnenc} and {@code mvnup}. */
    public static final int BAD_OPERATION = 2;

    /**
     * Nothing was rejected, but something was reported. Deliberately not 2: the CLI itself exits
     * with 2 when a tool fails in a way it does not handle, and a gate must be able to tell "this
     * POM has warnings" from "mvnval broke".
     */
    public static final int WARNINGS = 3;

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
        OutputFormat format;
        try {
            // The parser checks --format too, but options are interpolated after parsing, so
            // ${...} can still turn into anything by the time it gets here.
            format = context.options().format().map(OutputFormat::parse).orElse(OutputFormat.TEXT);
        } catch (IllegalArgumentException e) {
            context.logger.error(e.getMessage());
            return BAD_OPERATION;
        }

        List<Report> reports = validate(resolvePoms(context), createSession());
        // determineWriter, not context.writer: that field is lazy and nothing on this path has
        // created it yet.
        format.report(reports, determineWriter(context));
        return exitCode(reports);
    }

    /**
     * Loads no core extension, whatever {@code .mvn/extensions.xml} asks for.
     * <p>
     * The default selector resolves every declared extension before {@code execute} runs, which
     * reaches the network and then runs that extension's code. Validating a POM someone handed you
     * must not do either, and no extension can change what the raw model says anyway.
     */
    @Override
    protected CoreExtensionSelector<ValidateContext> createCoreExtensionSelector() {
        return (invoker, context) -> List.of();
    }

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

    private static List<Report> validate(List<Path> poms, Session session) {
        ModelBuilder modelBuilder = session.getService(ModelBuilder.class);
        List<Report> reports = new ArrayList<>(poms.size());
        for (Path pom : poms) {
            reports.add(validate(pom, session, modelBuilder));
        }
        return reports;
    }

    private static Report validate(Path pom, Session session, ModelBuilder modelBuilder) {
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
            return Report.of(pom, problemsOf(modelBuilder.newSession().buildRaw(request)));
        } catch (ModelBuilderException e) {
            // A result that explains nothing would report the file as clean, so the exception
            // speaks in its place.
            ModelBuilderResult result = e.getResult();
            return result != null && result.getProblemCollector().hasErrorProblems()
                    ? Report.of(pom, problemsOf(result))
                    : Report.failed(pom, describe(e));
        } catch (UnsupportedOperationException e) {
            // buildRaw is unimplemented: the tool is unusable, rather than this file being bad.
            throw e;
        } catch (Exception e) {
            // One pathological file must not lose the verdict on the others.
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
