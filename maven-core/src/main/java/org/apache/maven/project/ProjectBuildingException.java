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
package org.apache.maven.project;

import java.io.File;
import java.util.List;

/**
 * @author Jason van Zyl
 */
public class ProjectBuildingException
    extends Exception
{
    private final String projectId;

    private File pomFile;

    private List<ProjectBuildingResult> results;

    public ProjectBuildingException( String projectId, String message, Throwable cause )
    {
        super( createMessage( message, projectId, null ), cause );
        this.projectId = projectId;
    }

    /**
     * @param projectId
     * @param message
     * @param pomFile pom file location
     */
    public ProjectBuildingException( String projectId, String message, File pomFile )
    {
        super( createMessage( message, projectId, pomFile ) );
        this.projectId = projectId;
        this.pomFile = pomFile;
    }

    /**
     * @param projectId
     * @param message
     * @param pomFile pom file location
     * @param cause
     */
    protected ProjectBuildingException( String projectId, String message, File pomFile, Throwable cause )
    {
        super( createMessage( message, projectId, pomFile ), cause );
        this.projectId = projectId;
        this.pomFile = pomFile;
    }

    public ProjectBuildingException( List<ProjectBuildingResult> results )
    {
        super( "Some problems were encountered while processing the POMs" );
        this.projectId = "";
        this.results = results;
    }

    public File getPomFile()
    {
        return pomFile;
    }

    /**
     * @deprecated use {@link #getPomFile()}
     */
    public String getPomLocation()
    {
        if ( getPomFile() != null )
        {
            return getPomFile().getAbsolutePath();
        }
        else
        {
            return "null";
        }
    }

    public String getProjectId()
    {
        return projectId;
    }

    public List<ProjectBuildingResult> getResults()
    {
        return results;
    }

    private static String createMessage( String message, String projectId, File pomFile )
    {
        StringBuilder buffer = new StringBuilder( 256 );
        buffer.append( message );
        buffer.append( " for project " ).append( projectId );
        if ( pomFile != null )
        {
            buffer.append( " at " ).append( pomFile.getAbsolutePath() );
        }
        return buffer.toString();
    }

}
