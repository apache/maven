package org.apache.maven.plugin;

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

import org.apache.maven.project.DuplicateArtifactAttachmentException;
import org.apache.maven.project.MavenProject;

import static org.apache.commons.lang3.StringUtils.isNotEmpty;

/**
 * Exception in the plugin manager.
 */
public class PluginExecutionException
    extends PluginManagerException
{

    private final MojoExecution mojoExecution;

    public PluginExecutionException( MojoExecution mojoExecution, MavenProject project, String message )
    {
        super( mojoExecution.getMojoDescriptor(), project, message );
        this.mojoExecution = mojoExecution;
    }

    public PluginExecutionException( MojoExecution mojoExecution, MavenProject project, String message,
                                     Throwable cause )
    {
        super( mojoExecution.getMojoDescriptor(), project, message, cause );
        this.mojoExecution = mojoExecution;
    }

    public PluginExecutionException( MojoExecution mojoExecution, MavenProject project, Exception cause )
    {
        super( mojoExecution.getMojoDescriptor(), project, constructMessage( mojoExecution, cause ), cause );
        this.mojoExecution = mojoExecution;
    }

    public PluginExecutionException( MojoExecution mojoExecution, MavenProject project,
                                     DuplicateArtifactAttachmentException cause )
    {
        super( mojoExecution.getMojoDescriptor(), project, constructMessage( mojoExecution, cause ), cause );
        this.mojoExecution = mojoExecution;
    }

    public MojoExecution getMojoExecution()
    {
        return mojoExecution;
    }

    private static String constructMessage( MojoExecution mojoExecution, Throwable cause )
    {
        String message;

        if ( mojoExecution != null )
        {
            message =
                "Execution " + mojoExecution.getExecutionId() + " of goal " + mojoExecution.getMojoDescriptor().getId()
                    + " failed";
        }
        else
        {
            message = "Mojo execution failed";
        }

        if ( cause != null && isNotEmpty( cause.getMessage() ) )
        {
            message += ": " + cause.getMessage();
        }
        else
        {
            message += ".";
        }

        return message;
    }

}
