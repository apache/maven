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

/**
 * Exception that occurs when the project list contains duplicate projects instead of ignoring one.
 *
 * @author <a href="mailto:brett@apache.org">Brett Porter</a>
 */
public class DuplicateProjectException
    extends Exception
{
    private final String projectId;

    private final File existingProjectFile;

    private final File conflictingProjectFile;

    /**
     * @deprecated use {@link #DuplicateProjectException(String, File, File, String)}
     */
    public DuplicateProjectException( String message )
    {
        this( null, null, null, message );
    }

    /**
     * @deprecated use {@link #DuplicateProjectException(String, File, File, String)}
     */
    public DuplicateProjectException( String message, Exception e )
    {
        super( message, e );
        this.projectId = null;
        this.existingProjectFile = null;
        this.conflictingProjectFile = null;
    }

    public DuplicateProjectException( String projectId, File existingProjectFile, File conflictingProjectFile,
                                      String message )
    {
        super( message );
        this.projectId = projectId;
        this.existingProjectFile = existingProjectFile;
        this.conflictingProjectFile = conflictingProjectFile;
    }

    public String getProjectId()
    {
        return projectId;
    }

    public File getExistingProjectFile()
    {
        return existingProjectFile;
    }

    public File getConflictingProjectFile()
    {
        return conflictingProjectFile;
    }
}
