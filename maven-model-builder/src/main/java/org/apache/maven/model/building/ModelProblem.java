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
package org.apache.maven.model.building;

/**
 * Describes a problem that was encountered during model building. A problem can either be an exception that was thrown
 * or a simple string message. In addition, a problem carries a hint about its source, e.g. the POM file that exhibits
 * the problem.
 *
 * @author Benjamin Bentmann
 */
public interface ModelProblem {

    /**
     * The different severity levels for a problem, in decreasing order.
     */
    enum Severity {
        FATAL, //
        ERROR, //
        WARNING //
    }

    /**
     * Version
     */
    enum Version {
        // based on ModeBuildingResult.validationLevel
        BASE,
        V20,
        V30,
        V31
    }

    /**
     * Gets the hint about the source of the problem. While the syntax of this hint is unspecified and depends on the
     * creator of the problem, the general expectation is that the hint provides sufficient information to the user to
     * track the problem back to its origin. A concrete example for such a source hint can be the file path or URL from
     * which a POM was read.
     *
     * @return The hint about the source of the problem or an empty string if unknown, never {@code null}.
     */
    String getSource();

    /**
     * Gets the one-based index of the line containing the problem. The line number should refer to some text file that
     * is given by {@link #getSource()}.
     *
     * @return The one-based index of the line containing the problem or a non-positive value if unknown.
     */
    int getLineNumber();

    /**
     * Gets the one-based index of the column containing the problem. The column number should refer to some text file
     * that is given by {@link #getSource()}.
     *
     * @return The one-based index of the column containing the problem or non-positive value if unknown.
     */
    int getColumnNumber();

    /**
     * Gets the identifier of the model from which the problem originated. While the general form of this identifier is
     * <code>groupId:artifactId:version</code> the returned identifier need not be complete. The identifier is derived
     * from the information that is available at the point the problem occurs and as such merely serves as a best effort
     * to provide information to the user to track the problem back to its origin.
     *
     * @return The identifier of the model from which the problem originated or an empty string if unknown, never
     *         {@code null}.
     */
    String getModelId();

    /**
     * Gets the exception that caused this problem (if any).
     *
     * @return The exception that caused this problem or {@code null} if not applicable.
     */
    Exception getException();

    /**
     * Gets the message that describes this problem.
     *
     * @return The message describing this problem, never {@code null}.
     */
    String getMessage();

    /**
     * Gets the severity level of this problem.
     *
     * @return The severity level of this problem, never {@code null}.
     */
    Severity getSeverity();

    /**
     * Gets the applicable maven version/validation level of this problem
     * @return The version, never {@code null}.
     */
    Version getVersion();
}
