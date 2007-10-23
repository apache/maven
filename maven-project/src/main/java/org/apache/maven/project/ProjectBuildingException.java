package org.apache.maven.project;

import java.io.File;
import java.net.URI;

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

/**
 * @author <a href="mailto:jason@maven.org">Jason van Zyl</a>
 * @version $Id$
 */
public class ProjectBuildingException
    extends Exception
{
    private final String projectId;

    private URI pomUri;

    public ProjectBuildingException( String projectId, String message )
    {
        this( message, projectId, (File) null );
    }

    /**
     * @deprecated use {@link #ProjectBuildingException(String, String, File)}
     * @param projectId
     * @param message
     * @param pomLocation absolute path of the pom file
     */
    protected ProjectBuildingException( String projectId, String message, String pomLocation )
    {
        this( projectId, message, pomLocation, null );
    }

    /**
     * @deprecated use {@link #ProjectBuildingException(String, String, File, Throwable)}
     * @param projectId
     * @param message
     * @param pomLocation absolute path of the pom file
     * @param cause
     */
    public ProjectBuildingException( String projectId, String message, String pomLocation, Throwable cause )
    {
        this( projectId, message, new File( pomLocation ), null );
    }

    /**
     * 
     * @param projectId
     * @param message
     * @param pomFile pom file location
     */
    public ProjectBuildingException( String projectId, String message, File pomFile )
    {
        this( projectId, message, pomFile, null );
    }

    /**
     * 
     * @param projectId
     * @param message
     * @param cause
     */
    public ProjectBuildingException( String projectId, String message, Throwable cause )
    {
        this( projectId, message, (URI) null, cause );
    }

    /**
     * @param projectId
     * @param message
     * @param pomFile pom file location
     * @param cause
     */
    public ProjectBuildingException( String projectId, String message, File pomFile, Throwable cause )
    {
        this( projectId, message, pomFile.toURI(), cause );
    }

    /**
     * Equivalent to new ProjectBuildingException(projectId, message, pomUri, null)
     * @see #ProjectBuildingException(String, String, URI, Throwable)
     */
    public ProjectBuildingException( String projectId, String message, URI pomUri )
    {
        this( projectId, message, pomUri, null );
    }

    /**
     * @param projectId
     * @param message
     * @param pomUri location of the pom
     * @param cause
     */
    public ProjectBuildingException( String projectId, String message, URI pomUri, Throwable cause )
    {
        super( message, cause );
        this.projectId = projectId;
        this.pomUri = pomUri;
    }

    public URI getPomUri()
    {
        return pomUri;
    }

    /**
     * @deprecated use {@link #getPomUri()}
     */
    public String getPomLocation()
    {
        if ( "file".equals( getPomUri().getScheme() ) )
        {
            return new File( getPomUri() ).getAbsolutePath();
        }
        return getPomUri().toString();
    }

    public String getProjectId()
    {
        return projectId;
    }

    public String getMessage()
    {
        return super.getMessage() + " for project " + projectId
            + ( ( getPomUri() == null ? "" : " at " + getPomLocation() ) );
    }
}
