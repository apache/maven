package org.apache.maven.execution;

/*
 * Copyright 2001-2004 The Apache Software Foundation.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

import org.apache.maven.plugin.FailureResponse;

/**
 * @author <a href="mailto:jason@maven.org">Jason van Zyl</a>
 * @version $Id$
 */
public class MavenExecutionResponse
{
    private String failedGoal;

    private FailureResponse failureResponse;

    private Throwable exception;

    // ----------------------------------------------------------------------
    // Execution failure
    // ----------------------------------------------------------------------

    public void setExecutionFailure( String failedGoal, FailureResponse response )
    {
        this.failedGoal = failedGoal;

        this.failureResponse = response;
    }

    public boolean isExecutionFailure()
    {
        return ( failedGoal != null || exception != null );
    }

    public String getFailedGoal()
    {
        return failedGoal;
    }

    public void setFailedGoal( String failedGoal )
    {
        this.failedGoal = failedGoal;
    }

    public FailureResponse getFailureResponse()
    {
        return failureResponse;
    }

    public void setFailureResponse( FailureResponse failureResponse )
    {
        this.failureResponse = failureResponse;
    }

    // ----------------------------------------------------------------------
    // Error
    // ----------------------------------------------------------------------

    public Throwable getException()
    {
        return exception;
    }

    public void setException( Throwable exception )
    {
        this.exception = exception;
    }
}
