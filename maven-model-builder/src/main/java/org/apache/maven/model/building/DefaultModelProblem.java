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

    private final int lineNumber;

    private final int columnNumber;

    private final String modelId;

    private final String message;

    private final Exception exception;

    private final Severity severity;

    private final Version version;


    /**
     * Creates a new problem with the specified message and exception.
     *
     * @param message The message describing the problem, may be {@code null}.
     * @param severity The severity level of the problem, may be {@code null} to default to
     *            {@link ModelProblem.Severity#ERROR}.
     * @param source The source of the problem, may be {@code null}.
     * @param lineNumber The one-based index of the line containing the error or {@code -1} if unknown.
     * @param columnNumber The one-based index of the column containing the error or {@code -1} if unknown.
     * @param exception The exception that caused this problem, may be {@code null}.
     */
    //mkleint: does this need to be public?
    public DefaultModelProblem( String message, Severity severity, Version version, Model source, int lineNumber,
                                int columnNumber, Exception exception )
    {
        this( message, severity, version, ModelProblemUtils.toPath( source ), lineNumber, columnNumber,
              ModelProblemUtils.toId( source ), exception );
    }

    /**
     * Creates a new problem with the specified message and exception.
     *
     * @param message The message describing the problem, may be {@code null}.
     * @param severity The severity level of the problem, may be {@code null} to default to
     *            {@link ModelProblem.Severity#ERROR}.
     * @param version The version since the problem is relevant
     * @param source A hint about the source of the problem like a file path, may be {@code null}.
     * @param lineNumber The one-based index of the line containing the problem or {@code -1} if unknown.
     * @param columnNumber The one-based index of the column containing the problem or {@code -1} if unknown.
     * @param modelId The identifier of the model that exhibits the problem, may be {@code null}.
     * @param exception The exception that caused this problem, may be {@code null}.
     */
    //mkleint: does this need to be public?
    @SuppressWarnings( "checkstyle:parameternumber" )
    public DefaultModelProblem( String message, Severity severity, Version version, String source, int lineNumber,
                                int columnNumber, String modelId, Exception exception )
    {
        this.message = message;
        this.severity = ( severity != null ) ? severity : Severity.ERROR;
        this.source = ( source != null ) ? source : "";
        this.lineNumber = lineNumber;
        this.columnNumber = columnNumber;
        this.modelId = ( modelId != null ) ? modelId : "";
        this.exception = exception;
        this.version = version;
    }

    @Override
    public String getSource()
    {
        return source;
    }

    @Override
    public int getLineNumber()
    {
        return lineNumber;
    }

    @Override
    public int getColumnNumber()
    {
        return columnNumber;
    }

    @Override
    public String getModelId()
    {
        return modelId;
    }

    @Override
    public Exception getException()
    {
        return exception;
    }

    @Override
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
    public Severity getSeverity()
    {
        return severity;
    }

    @Override
    public Version getVersion()
    {
        return version;
    }


    @Override
    public String toString()
    {
        StringBuilder buffer = new StringBuilder( 128 );

        buffer.append( '[' ).append( getSeverity() ).append( "] " );
        buffer.append( getMessage() );
        String location = ModelProblemUtils.formatLocation( this, null );
        if ( !location.isEmpty() )
        {
            buffer.append( " @ " );
            buffer.append( location );
        }

        return buffer.toString();
    }

}
