package org.apache.maven.project;

import org.apache.maven.artifact.InvalidRepositoryException;
import org.apache.maven.artifact.resolver.ArtifactNotFoundException;
import org.apache.maven.artifact.resolver.ArtifactResolutionException;
import org.apache.maven.artifact.versioning.InvalidVersionSpecificationException;
import org.apache.maven.profiles.activation.ProfileActivationException;
import org.apache.maven.project.artifact.InvalidDependencyVersionException;
import org.apache.maven.project.interpolation.ModelInterpolationException;
import org.codehaus.plexus.util.xml.pull.XmlPullParserException;

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
 * @author <a href="mailto:jason@maven.org">Jason van Zyl</a>
 * @version $Id$
 */
public class ProjectBuildingException
    extends Exception
{
    private final String projectId;

    private String pomLocation;

    public ProjectBuildingException( String projectId,
                                     String message )
    {
        super( message );
        this.projectId = projectId;
    }

    protected ProjectBuildingException( String projectId,
                                        String message,
                                        String pomLocation )
    {
        super( message );
        this.projectId = projectId;
        this.pomLocation = pomLocation;
    }

    public ProjectBuildingException( String projectId,
                                     String message,
                                     String pomLocation,
                                     ProfileActivationException cause )
    {
        super( message, cause );
        this.projectId = projectId;
        this.pomLocation = pomLocation;
    }

    public ProjectBuildingException( String projectId,
                                     String message,
                                     String pomLocation,
                                     IOException cause )
    {
        super( message, cause );
        this.projectId = projectId;
        this.pomLocation = pomLocation;
    }

    public ProjectBuildingException( String projectId,
                                     String message,
                                     String pomLocation,
                                     XmlPullParserException cause )
    {
        super( message, cause );
        this.projectId = projectId;
        this.pomLocation = pomLocation;
    }

    protected ProjectBuildingException( String projectId,
                                     String message,
                                     XmlPullParserException cause )
    {
        super( message, cause );
        this.projectId = projectId;
    }

    public ProjectBuildingException( String projectId,
                                     String message,
                                     String pomLocation,
                                     InvalidRepositoryException cause )
    {
        super( message, cause );
        this.projectId = projectId;
        this.pomLocation = pomLocation;
    }

    public ProjectBuildingException( String projectId,
                                     String message,
                                     InvalidRepositoryException cause )
    {
        super( message, cause );
        this.projectId = projectId;
    }

    public ProjectBuildingException( String projectId,
                                     String message,
                                     ArtifactResolutionException cause )
    {
        super( message, cause );
        this.projectId = projectId;
    }

    public ProjectBuildingException( String projectId,
                                     String message,
                                     ArtifactNotFoundException cause )
    {
        super( message, cause );
        this.projectId = projectId;
    }

    public ProjectBuildingException( String projectId,
                                     String message,
                                     String pomLocation,
                                     ArtifactResolutionException cause )
    {
        super( message, cause );
        this.projectId = projectId;
        this.pomLocation = pomLocation;
    }

    public ProjectBuildingException( String projectId,
                                     String message,
                                     String pomLocation,
                                     ArtifactNotFoundException cause )
    {
        super( message, cause );
        this.projectId = projectId;
        this.pomLocation = pomLocation;
    }

    public ProjectBuildingException( String projectId,
                                     String message,
                                     String pomLocation,
                                     InvalidVersionSpecificationException cause )
    {
        super( message, cause );
        this.projectId = projectId;
        this.pomLocation = pomLocation;
    }

    public ProjectBuildingException( String projectId,
                                     String message,
                                     String pomLocation,
                                     InvalidDependencyVersionException cause )
    {
        super( message, cause );
        this.projectId = projectId;
        this.pomLocation = pomLocation;
    }

    protected ProjectBuildingException( String projectId,
                                        String message,
                                        String pomLocation,
                                        ModelInterpolationException cause )
    {
        super( message, cause );
        this.projectId = projectId;
        this.pomLocation = pomLocation;
    }

    public String getPomLocation()
    {
        return pomLocation;
    }

    public String getProjectId()
    {
        return projectId;
    }

    public String getMessage()
    {
        return super.getMessage() + " for project " + projectId
               + ( ( pomLocation == null ? "" : " at " + pomLocation ) );
    }

}
