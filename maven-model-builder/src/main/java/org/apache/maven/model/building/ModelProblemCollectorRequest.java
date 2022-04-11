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

import java.util.Objects;

import org.apache.maven.model.InputLocation;
import org.apache.maven.model.building.ModelProblem.Severity;
import org.apache.maven.model.building.ModelProblem.Version;

/**
 * Class to wrap request parameters to ModelProblemCollector.addProblem
 *
 * @author mkleint
 */
public final class ModelProblemCollectorRequest
{

    private final ModelProblem.Severity severity;
    private final ModelProblem.Version version;
    private Exception exception;
    private String message;
    private InputLocation location;

    /**
     * Create a new request with mandatory parameters.
     * @param severity
     * @param version
     */
    public ModelProblemCollectorRequest( Severity severity, Version version )
    {
        this.severity = Objects.requireNonNull( severity, "severity cannot be null" );
        this.version = Objects.requireNonNull( version, "version cannot be null" );
    }

    public Severity getSeverity()
    {
        return severity;
    }

    public Version getVersion()
    {
        return version;
    }

    public Exception getException()
    {
        return exception;
    }

    public ModelProblemCollectorRequest setException( Exception exception )
    {
        this.exception = exception;
        return this;
    }

    public String getMessage()
    {
        return message;
    }

    public ModelProblemCollectorRequest setMessage( String message )
    {
        this.message = message;
        return this;
    }

    public InputLocation getLocation()
    {
        return location;
    }

    public ModelProblemCollectorRequest setLocation( InputLocation location )
    {
        this.location = location;
        return this;
    }
}
