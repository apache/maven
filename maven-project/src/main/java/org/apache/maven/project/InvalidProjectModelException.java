package org.apache.maven.project;

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

import java.io.File;
import java.net.URI;

import org.apache.maven.project.validation.ModelValidationResult;

public class InvalidProjectModelException
    extends ProjectBuildingException
{
    private ModelValidationResult validationResult;

    /**
     * 
     * @param projectId
     * @param message
     * @param pomFile pom file
     * @param cause
     */
    public InvalidProjectModelException( String projectId,
                                         String message,
                                         File pomFile,
                                         Throwable cause )
    {
        super( projectId, message, pomFile, cause );
    }

    /**
     * 
     * @param projectId
     * @param message
     * @param pomLocation pom location
     * @param cause
     */
    public InvalidProjectModelException( String projectId,
                                         String message,
                                         URI pomLocation,
                                         Throwable cause )
    {
        super( projectId, message, pomLocation, cause );
    }

    /**
     * 
     * @param projectId
     * @param message
     * @param pomLocation pom location
     */
    public InvalidProjectModelException( String projectId,
                                         String message,
                                         URI pomLocation )
    {
        super( projectId, message, pomLocation );
    }

    /**
     * @deprecated use {@link #InvalidProjectModelException(String, String, File, Throwable)}
     * @param projectId
     * @param pomLocation absolute path of the pom file
     * @param message
     * @param cause
     */
    public InvalidProjectModelException( String projectId,
                                         String pomLocation,
                                         String message,
                                         Throwable cause )
    {
        super( projectId, message, new File( pomLocation ), cause );
    }

    /**
     * @deprecated use {@link #InvalidProjectModelException(String, String, File, ModelValidationResult)}
     * @param projectId
     * @param pomLocation absolute path of the pom file
     * @param message
     * @param validationResult
     */
    public InvalidProjectModelException( String projectId,
                                         String pomLocation,
                                         String message,
                                         ModelValidationResult validationResult )
    {
        this( projectId, message, new File( pomLocation ), validationResult );
    }

    public InvalidProjectModelException( String projectId,
                                         String message,
                                         File pomFile,
                                         ModelValidationResult validationResult )
    {
        super( projectId, message, pomFile );

        this.validationResult = validationResult;
    }

    public InvalidProjectModelException( String projectId,
                                         String message,
                                         File pomLocation )
    {
        super( projectId, message, pomLocation );
    }

    /**
     * @deprecated use {@link #InvalidProjectModelException(String, String, File)}
     * @param projectId
     * @param pomLocation absolute path of the pom file
     * @param message
     */
    public InvalidProjectModelException( String projectId,
                                         String pomLocation,
                                         String message )
    {
        super( projectId, message, new File( pomLocation ) );
    }

    public final ModelValidationResult getValidationResult()
    {
        return validationResult;
    }

}
