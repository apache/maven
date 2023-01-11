package org.apache.maven.plugin.coreit;

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

import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugins.annotations.Component;
import org.apache.maven.project.MavenProject;
import org.apache.maven.project.MavenProjectBuilder;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.Properties;

/**
 * @author Benjamin Bentmann
 */
public abstract class AbstractPomMojo
    extends AbstractMojo
{

    /**
     * The project builder.
     */
    @Component
    protected MavenProjectBuilder builder;

    protected void dump( Properties props, String key, MavenProject project )
    {
        put( props, key + "project.id", project.getId() );
        put( props, key + "project.name", project.getName() );
        put( props, key + "project.description", project.getDescription() );
        if ( project.getArtifact() != null )
        {
            put( props, key + "artifact.id", project.getArtifact().getId() );
        }
    }

    protected void put( Properties props, String key, Object value )
    {
        if ( value != null )
        {
            props.setProperty( key, value.toString() );
        }
    }

    protected void store( Properties props, File file )
        throws MojoExecutionException
    {
        try
        {
            file.getParentFile().mkdirs();

            try ( FileOutputStream os = new FileOutputStream( file ) )
            {
                props.store( os, "[MAVEN-CORE-IT-LOG]" );
            }
        }
        catch ( IOException e )
        {
            throw new MojoExecutionException( "Failed to dump POMs: " + e.getMessage(), e );
        }
    }

}
