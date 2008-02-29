package org.apache.maven.project;

import org.apache.maven.artifact.InvalidRepositoryException;
import org.apache.maven.artifact.resolver.ArtifactNotFoundException;
import org.apache.maven.artifact.resolver.ArtifactResolutionException;
import org.apache.maven.artifact.versioning.InvalidVersionSpecificationException;
import org.apache.maven.profiles.activation.ProfileActivationException;
import org.apache.maven.project.artifact.InvalidDependencyVersionException;
import org.apache.maven.project.interpolation.ModelInterpolationException;
import org.codehaus.plexus.util.xml.pull.XmlPullParserException;

import java.io.File;
import java.io.IOException;

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
 * @author Jason van Zyl
 * @version $Id$
 */
public class ProjectBuildingException
    extends Exception
{
    private final String projectId;

    private File pomFile;

    public ProjectBuildingException( String projectId, String message )
    {
        super( message );
        this.projectId = projectId;
    }

    /**
     * @deprecated use {@link File} constructor for pomLocation
     * @param projectId
     * @param message
     * @param pomLocation absolute path of the pom file
     */
    protected ProjectBuildingException( String projectId, String message, String pomLocation )
    {
        super( message );
        this.projectId = projectId;
        pomFile = new File( pomLocation );
    }

    /**
     *
     * @param projectId
     * @param message
     * @param pomFile pom file location
     */
    public ProjectBuildingException( String projectId, String message, File pomFile )
    {
        super( message );
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
        super( message, cause );
        this.projectId = projectId;
        this.pomFile = pomFile;
    }

    /**
     * @deprecated use {@link File} constructor for pomLocation
     */
    public ProjectBuildingException( String projectId, String message, String pomLocation,
                                     ProfileActivationException cause )
    {
        super( message, cause );
        this.projectId = projectId;
        pomFile = new File( pomLocation );
    }

    public ProjectBuildingException( String projectId, String message, File pomFile,
                                     ProfileActivationException cause )
    {
        super( message, cause );
        this.projectId = projectId;
        this.pomFile = pomFile;
    }

    /**
     * @deprecated use {@link File} constructor for pomLocation
     */
    public ProjectBuildingException( String projectId, String message, String pomLocation, IOException cause )
    {
        super( message, cause );
        this.projectId = projectId;
        pomFile = new File( pomLocation );
    }

    public ProjectBuildingException( String projectId, String message, File pomFile, IOException cause )
    {
        super( message, cause );
        this.projectId = projectId;
        this.pomFile = pomFile;
    }

    // for super-POM building.
    public ProjectBuildingException( String projectId, String message, IOException cause )
    {
        super( message, cause );
        this.projectId = projectId;
    }

    /**
     * @deprecated use {@link File} constructor for pomLocation
     */
    public ProjectBuildingException( String projectId, String message, String pomLocation, XmlPullParserException cause )
    {
        super( message, cause );
        this.projectId = projectId;
        pomFile = new File( pomLocation );
    }

    public ProjectBuildingException( String projectId, String message, File pomFile, XmlPullParserException cause )
    {
        super( message, cause );
        this.projectId = projectId;
        this.pomFile = pomFile;
    }

    protected ProjectBuildingException( String projectId, String message, XmlPullParserException cause )
    {
        super( message, cause );
        this.projectId = projectId;
    }

    public ProjectBuildingException( String projectId, String message, ArtifactResolutionException cause )
    {
        super( message, cause );
        this.projectId = projectId;
    }

    public ProjectBuildingException( String projectId, String message, InvalidRepositoryException cause )
    {
        super( message, cause );
        this.projectId = projectId;
    }

    public ProjectBuildingException( String projectId, String message, File pomFile, InvalidRepositoryException cause )
    {
        super( message, cause );
        this.projectId = projectId;
        this.pomFile = pomFile;
    }

    public ProjectBuildingException( String projectId, String message, ArtifactNotFoundException cause )
    {
        super( message, cause );
        this.projectId = projectId;
    }

    public ProjectBuildingException( String projectId, String message, File pomFile,
                                     ArtifactResolutionException cause )
    {
        super( message, cause );
        this.projectId = projectId;
        this.pomFile = pomFile;
    }

    /**
     * @deprecated use {@link File} constructor for pomLocation
     */
    public ProjectBuildingException( String projectId, String message, String pomLocation,
                                     ArtifactResolutionException cause )
    {
        super( message, cause );
        this.projectId = projectId;
        pomFile = new File( pomLocation );
    }

    public ProjectBuildingException( String projectId, String message, File pomFile, ArtifactNotFoundException cause )
    {
        super( message, cause );
        this.projectId = projectId;
        this.pomFile = pomFile;
    }

    /**
     * @deprecated use {@link File} constructor for pomLocation
     */
    public ProjectBuildingException( String projectId, String message, String pomLocation,
                                     ArtifactNotFoundException cause )
    {
        super( message, cause );
        this.projectId = projectId;
        pomFile = new File( pomLocation );
    }

    public ProjectBuildingException( String projectId, String message, File pomFile,
                                     InvalidVersionSpecificationException cause )
    {
        super( message, cause );
        this.projectId = projectId;
        this.pomFile = pomFile;
    }

    /**
     * @deprecated use {@link File} constructor for pomLocation
     */
    public ProjectBuildingException( String projectId, String message, String pomLocation,
                                     InvalidVersionSpecificationException cause )
    {
        super( message, cause );
        this.projectId = projectId;
        pomFile = new File( pomLocation );
    }

    public ProjectBuildingException( String projectId, String message, File pomFile,
                                     InvalidDependencyVersionException cause )
    {
        super( message, cause );
        this.projectId = projectId;
        this.pomFile = pomFile;
    }

    /**
     * @deprecated use {@link File} constructor for pomLocation
     */
    public ProjectBuildingException( String projectId, String message, String pomLocation,
                                     InvalidDependencyVersionException cause )
    {
        super( message, cause );
        this.projectId = projectId;
        pomFile = new File( pomLocation );
    }

    protected ProjectBuildingException( String projectId, String message, File pomFile,
                                        ModelInterpolationException cause )
    {
        super( message, cause );
        this.projectId = projectId;
        this.pomFile = pomFile;
    }

    /**
     * @deprecated use {@link File} constructor for pomLocation
     */
    protected ProjectBuildingException( String projectId, String message, String pomLocation,
                                        ModelInterpolationException cause )
    {
        super( message, cause );
        this.projectId = projectId;
        pomFile = new File ( pomLocation );
    }

    public ProjectBuildingException( String projectId,
                                     String message,
                                     ModelInterpolationException cause )
    {
        super( message, cause );
        this.projectId = projectId;
    }

    public File getPomFile()
    {
        return pomFile;
    }

    /**
     * @deprecated use {@link #getPomFile()}
     */
    public String getPomLocation ()
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

    public String getMessage()
    {
        return super.getMessage() + " for project " + projectId
            + ( ( getPomFile() == null ? "" : " at " + getPomFile().getAbsolutePath() ) );
    }
}
