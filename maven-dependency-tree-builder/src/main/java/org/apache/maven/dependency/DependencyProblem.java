package org.apache.maven.dependency;

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

/**
 * Describes a problem that was encountered during model building. A problem can either be an exception that was thrown
 * or a simple string message. In addition, a problem carries a hint about its source, e.g. the dependency that exhibits
 * the problem.
 * 
 * @author Benjamin Bentmann
 */
public interface DependencyProblem
{

    /**
     * The different severity levels for a problem, in decreasing order.
     */
    enum Severity
    {

        ERROR, //
        WARNING; //

    }

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

}
