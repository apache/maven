package org.apache.maven.plugin;

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

/**
 * @author <a href="mailto:jason@maven.org">Jason van Zyl</a>
 * @version $Id$
 */
public class PluginExecutionResponse
{
    private FailureResponse failureResponse = null;

    public boolean isExecutionFailure()
    {
        return failureResponse != null;
    }

    public void setExecutionFailure( FailureResponse failureResponse )
    {
        this.failureResponse = failureResponse;
    }

    /**
     * @deprecated please use {@link #setExecutionFailure(FailureResponse)} as there is no need to set executionFailure to false if there is a failure response
     */
    public void setExecutionFailure( boolean executionFailure, FailureResponse failureResponse )
    {
        if ( executionFailure == false )
        {
            throw new IllegalArgumentException( "executionFailure should be true when passing a failureResponse" );
        }

        setExecutionFailure( failureResponse );
    }

    public FailureResponse getFailureResponse()
    {
        return failureResponse;
    }
}
