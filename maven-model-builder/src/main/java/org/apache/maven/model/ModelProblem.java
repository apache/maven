package org.apache.maven.model;

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
 * or a simple string message. In addition, a problem carries a hint about its source, e.g. the POM file that could not
 * be processed.
 * 
 * @author Benjamin Bentmann
 */
public class ModelProblem
{

    private String source;

    private String message;

    private Exception exception;

    /**
     * Creates a new problem with the specified message.
     * 
     * @param message The message describing the problem, may be {@code null}.
     * @param source A hint about the source of the problem, may be {@code null}.
     */
    public ModelProblem( String message, String source )
    {
        this.message = message;
        this.source = ( source != null ) ? source : "";
    }

    /**
     * Creates a new problem with the specified message and exception.
     * 
     * @param message The message describing the problem, may be {@code null}.
     * @param source A hint about the source of the problem, may be {@code null}.
     * @param exception The exception that caused this problem, may be {@code null}.
     */
    public ModelProblem( String message, String source, Exception exception )
    {
        this.message = message;
        this.source = ( source != null ) ? source : "";
        this.exception = exception;
    }

    /**
     * Gets the hint about the source of the problem. While the syntax of this hint is unspecified and depends on the
     * creator of the problem, the general expectation is that the hint provides sufficient information to the user to
     * track the problem back to its origin.
     * 
     * @return The hint about the source of the problem, never {@code null}.
     */
    public String getSource()
    {
        return source;
    }

    /**
     * Gets the exception that caused this problem (if any).
     * 
     * @return The exception that caused this problem or {@code null} if not applicable.
     */
    public Exception getException()
    {
        return exception;
    }

    /**
     * Gets the message that describes this problem.
     * 
     * @return The message describing this problem, never {@code null}.
     */
    public String getMessage()
    {
        String msg;

        if ( message != null && message.length() > 0 )
        {
            msg = message;
        }
        else
        {
            msg = exception.getMessage();

            if ( msg == null )
            {
                msg = "";
            }
        }

        return msg;
    }

    @Override
    public String toString()
    {
        return getSource() + ": " + getMessage();
    }

}
