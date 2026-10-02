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

import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import org.apache.maven.api.cli.InvokerRequest;
import org.apache.maven.api.cli.Logger;
import org.apache.maven.api.cli.ParserRequest;
import org.apache.maven.api.cli.mvnval.ValidateOptions;
import org.apache.maven.api.services.BuilderProblem;
import org.apache.maven.api.services.ModelProblem;
import org.apache.maven.impl.model.DefaultModelProblem;
import org.jline.terminal.Terminal;

import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

final class TestUtils {

    private TestUtils() {}

    /**
     * Creates a problem the way the model builder does, through {@link DefaultModelProblem}.
     * Fakes must not be used here: {@code DefaultModelProblem.getLocation()} returns an empty
     * string, and a fake that returns a location hides the bug that fact caused.
     */
    static ModelProblem problem(BuilderProblem.Severity severity, String message, String source, int line, int column) {
        return new DefaultModelProblem(
                message, severity, ModelProblem.Version.V40, source, line, column, "org.test:test:1.0", null);
    }

    /** Collects the lines a reporter writes, in order, as if run from an unrelated directory. */
    static List<String> render(OutputFormat format, List<Report> reports) {
        List<String> lines = new ArrayList<>();
        format.report(reports, Paths.get("/elsewhere"), lines::add);
        return lines;
    }

    static ValidateContext createMockContext(Path workingDirectory, ValidateOptions options) {
        InvokerRequest request = mock(InvokerRequest.class);

        when(request.cwd()).thenReturn(workingDirectory);
        when(request.installationDirectory()).thenReturn(Paths.get("/maven"));
        when(request.userHomeDirectory()).thenReturn(Paths.get("/home/user"));
        when(request.topDirectory()).thenReturn(workingDirectory);
        when(request.rootDirectory()).thenReturn(Optional.empty());
        when(request.userProperties()).thenReturn(Map.of());
        when(request.systemProperties()).thenReturn(Map.of());
        when(request.options()).thenReturn(Optional.ofNullable(options));

        ParserRequest parserRequest = mock(ParserRequest.class);
        Logger logger = mock(Logger.class);
        doAnswer(invocation -> {
                    System.err.println("[ERROR] " + invocation.getArgument(0));
                    return null;
                })
                .when(logger)
                .error(anyString());

        when(request.parserRequest()).thenReturn(parserRequest);
        when(parserRequest.logger()).thenReturn(logger);

        ValidateContext context = new ValidateContext(request, options);
        // LookupInvoker.createTerminal fills this in on a real run; the invoker installs its
        // interrupt handler on it, so a test driving execute() directly needs one here.
        context.terminal = mock(Terminal.class);
        return context;
    }
}
