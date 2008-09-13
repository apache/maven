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

import org.apache.maven.artifact.InvalidRepositoryException;
import org.apache.maven.project.validation.ModelValidationResult;
import org.codehaus.plexus.util.xml.pull.XmlPullParserException;

import java.io.File;

public class InvalidProjectModelException
    extends ProjectBuildingException
{
    private ModelValidationResult validationResult;

    public InvalidProjectModelException( String projectId, String message, File pomLocation )
    {
        super( projectId, message, pomLocation );
    }

    /**
     * @param projectId
     * @param pomLocation      absolute path of the pom file
     * @param message
     * @param validationResult
     * @deprecated use {@link File} constructor for pomLocation
     */
    public InvalidProjectModelException( String projectId, String pomLocation, String message,
                                         ModelValidationResult validationResult )
    {
        this( projectId, message, new File( pomLocation ), validationResult );
    }

    public InvalidProjectModelException( String projectId, String message, File pomFile,
                                         ModelValidationResult validationResult )
    {
        super( projectId, message, pomFile );

        this.validationResult = validationResult;
    }

    /**
     * @param projectId
     * @param pomLocation absolute path of the pom file
     * @param message
     * @deprecated use {@link File} constructor for pomLocation
     */
    public InvalidProjectModelException( String projectId, String pomLocation, String message )
    {
        this( projectId, message, new File( pomLocation ) );
    }


    /**
     * @deprecated use {@link File} constructor for pomLocation
     */
    public InvalidProjectModelException( String projectId, String pomLocation, String message,
                                         InvalidRepositoryException cause )
    {
        this( projectId, message, new File( pomLocation ), cause );
    }

    public InvalidProjectModelException( String projectId, String message, File pomLocation,
                                         InvalidRepositoryException cause )
    {
        super( projectId, message, pomLocation, cause );
    }

    /**
     * @deprecated use {@link File} constructor for pomLocation
     */
    public InvalidProjectModelException( String projectId, String pomLocation, String message,
                                         XmlPullParserException cause )
    {
        this( projectId, message, new File( pomLocation ), cause );
    }

    public InvalidProjectModelException( String projectId, String message, File pomFile, XmlPullParserException cause )
    {
        super( projectId, message, pomFile, cause );
    }

    // for super-POM building.
    public InvalidProjectModelException( String projectId, String message, XmlPullParserException cause )
    {
        super( projectId, message, cause );
    }

    public final ModelValidationResult getValidationResult()
    {
        return validationResult;
    }

}
