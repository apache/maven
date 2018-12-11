package org.apache.maven.exception;

/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *  http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */

import java.util.Collections;
import java.util.List;

/**
 * Provide a summary of the exception, containing:<ul>
 * <li>the exception itself,</li>
 * <li>useful end-user message,</li>
 * <li>useful reference to a solution, or set of solutions: this is usually a wiki page url in
 * <a href="http://cwiki.apache.org/confluence/display/MAVEN/">http://cwiki.apache.org/confluence/display/MAVEN/</a>,
 * </li>
 * <li>child exception summaries.</li>
 * </ul>
 */
public class ExceptionSummary
{

    private Throwable exception;

    private String message;

    private String reference;

    private List<ExceptionSummary> children;

    public ExceptionSummary( Throwable exception, String message, String reference )
    {
        this( exception, message, reference, null );
    }

    public ExceptionSummary( Throwable exception, String message, String reference, List<ExceptionSummary> children )
    {
        this.exception = exception;
        this.message = ( message != null ) ? message : "";
        this.reference = ( reference != null ) ? reference : "";
        this.children = ( children != null )
                            ? Collections.unmodifiableList( children )
                            : Collections.<ExceptionSummary>emptyList();

    }

    public Throwable getException()
    {
        return exception;
    }

    public String getMessage()
    {
        return message;
    }

    public String getReference()
    {
        return reference;
    }

    public List<ExceptionSummary> getChildren()
    {
        return children;
    }

}
