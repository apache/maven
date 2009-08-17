package org.apache.maven.model.building;

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

import org.apache.maven.model.Model;

/**
 * Describes a problem that was encountered during model building. A problem can either be an exception that was thrown
 * or a simple string message. In addition, a problem carries a hint about its source, e.g. the POM file that exhibits
 * the problem.
 * 
 * @author Benjamin Bentmann
 */
public class DefaultModelProblem
    implements ModelProblem
{

    private final String source;

    private final String message;

    private final Exception exception;

    private final Severity severity;

    /**
     * Creates a new problem with the specified message.
     * 
     * @param message The message describing the problem, may be {@code null}.
     * @param severity The severity level of the problem, may be {@code null} to default to {@link Severity#ERROR}.
     * @param source The source of the problem, may be {@code null}.
     */
    public DefaultModelProblem( String message, Severity severity, Model source )
    {
        this( message, severity, ModelProblemUtils.toSourceHint( source ), null );
    }

    /**
     * Creates a new problem with the specified message.
     * 
     * @param message The message describing the problem, may be {@code null}.
     * @param severity The severity level of the problem, may be {@code null} to default to {@link Severity#ERROR}.
     * @param source A hint about the source of the problem, may be {@code null}.
     */
    public DefaultModelProblem( String message, Severity severity, String source )
    {
        this( message, severity, source, null );
    }

    /**
     * Creates a new problem with the specified message and exception.
     * 
     * @param message The message describing the problem, may be {@code null}.
     * @param severity The severity level of the problem, may be {@code null} to default to {@link Severity#ERROR}.
     * @param source A hint about the source of the problem, may be {@code null}.
     * @param exception The exception that caused this problem, may be {@code null}.
     */
    public DefaultModelProblem( String message, Severity severity, String source, Exception exception )
    {
        this.message = message;
        this.severity = ( severity != null ) ? severity : Severity.ERROR;
        this.source = ( source != null ) ? source : "";
        this.exception = exception;
    }

    public String getSource()
    {
        return source;
    }

    public Exception getException()
    {
        return exception;
    }

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

    public Severity getSeverity()
    {
        return severity;
    }

    @Override
    public String toString()
    {
        StringBuilder buffer = new StringBuilder( 128 );

        buffer.append( "[" ).append( getSeverity() ).append( "] " );
        buffer.append( getSource() ).append( ": " ).append( getMessage() );

        return buffer.toString();
    }

}
