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

import org.apache.maven.artifact.Artifact;

/**
 * This exception is thrown if an application attempts to attach
 * two of the same artifacts to a single project.
 *
 * @author pgier
 * @author jdcasey
 * @todo Make this a checked exception, and modify the API of MavenProjectHelper.
 * Currently, this modification would create compatibility problems for existing plugins.
 */
public class DuplicateArtifactAttachmentException
    extends RuntimeException
{

    private static final String DEFAULT_MESSAGE = "Duplicate artifact attachment detected.";

    private Artifact artifact;

    private final MavenProject project;

    public DuplicateArtifactAttachmentException( MavenProject project, Artifact artifact )
    {
        super( constructMessage( project, artifact ) );
        this.project = project;
        this.artifact = artifact;
    }

    private static String constructMessage( MavenProject project, Artifact artifact )
    {
        return DEFAULT_MESSAGE + " (project: " + project.getId() + "; illegal attachment: " + artifact.getId() + ")";
    }

    public MavenProject getProject()
    {
        return project;
    }

    public Artifact getArtifact()
    {
        return artifact;
    }
}
