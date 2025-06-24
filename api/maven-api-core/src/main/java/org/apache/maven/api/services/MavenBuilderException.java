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

    /**
     * The collection of problems associated with this exception.
     */
    private final ProblemCollector<BuilderProblem> problems;

    /**
     * Constructs a new exception with the specified message and cause.
     * This constructor creates an empty problem collector.
     *
     * @param message the detail message
     * @param cause the cause of this exception
     */
    public MavenBuilderException(String message, Throwable cause) {
        super(message, cause);
        problems = ProblemCollector.empty();
    }

    /**
     * Constructs a new exception with the specified message and problems.
     * The message will be enhanced with details from the problems.
     *
     * @param message the detail message
     * @param problems the collection of problems associated with this exception
     */
    public MavenBuilderException(String message, ProblemCollector<BuilderProblem> problems) {
        super(buildMessage(message, problems), null);
        this.problems = problems;
    }

    /**
     * Formats message out of problems: problems are sorted (in natural order of {@link BuilderProblem.Severity})
     * and then a list is built. These exceptions are usually thrown in "fatal" cases (and usually prevent Maven
     * from starting), and these exceptions may end up very early on output.
     *
     * @param message the base message to enhance
     * @param problems the collection of problems to include in the message
     * @return a formatted message including details of all problems
     */
    protected static String buildMessage(String message, ProblemCollector<BuilderProblem> problems) {
        StringBuilder msg = new StringBuilder(message);
        problems.problems().forEach(problem -> msg.append("\n * ")
                .append(problem.getSeverity().name())
                .append(": ")
                .append(problem.getMessage()));
        return msg.toString();
    }

    /**
     * Returns the problem collector associated with this exception.
     *
     * @return the problem collector containing all problems related to this exception
     */
    public ProblemCollector<BuilderProblem> getProblemCollector() {
        return problems;
    }
}
