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
package org.apache.maven.api.services;

import org.apache.maven.api.annotations.Experimental;

/**
 * Base class for all maven exceptions carrying {@link BuilderProblem}s.
 *
 * @since 4.0.0
 */
@Experimental
public abstract class MavenBuilderException extends MavenException {

    private final ProblemCollector<BuilderProblem> problems;

    public MavenBuilderException(String message, Throwable cause) {
        super(message, cause);
        problems = ProblemCollector.empty();
    }

    public MavenBuilderException(String message, ProblemCollector<BuilderProblem> problems) {
        super(buildMessage(message, problems), null);
        this.problems = problems;
    }

    /**
     * Formats message out of problems: problems are sorted (in natural order of {@link BuilderProblem.Severity})
     * and then a list is built. These exceptions are usually thrown in "fatal" cases (and usually prevent Maven
     * from starting), and these exceptions may end up very early on output.
     */
    protected static String buildMessage(String message, ProblemCollector<BuilderProblem> problems) {
        StringBuilder msg = new StringBuilder(message);
        problems.problems().forEach(problem -> msg.append("\n * ")
                .append(problem.getSeverity().name())
                .append(": ")
                .append(problem.getMessage()));
        return msg.toString();
    }

    public ProblemCollector<BuilderProblem> getProblemCollector() {
        return problems;
    }
}
