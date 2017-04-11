package org.apache.maven.its.mng6209.multiple.build.extensions.plugina;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.io.Writer;
import java.util.Set;
import java.util.TreeSet;

/*
 * Licensed to the Apache Software Foundation (ASF) under one or more contributor license
 * agreements. See the NOTICE file distributed with this work for additional information regarding
 * copyright ownership. The ASF licenses this file to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance with the License. You may obtain a
 * copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software distributed under the License
 * is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express
 * or implied. See the License for the specific language governing permissions and limitations under
 * the License.
 */

import javax.inject.Named;
import javax.inject.Singleton;

import org.apache.maven.AbstractMavenLifecycleParticipant;
import org.apache.maven.MavenExecutionException;
import org.apache.maven.execution.MavenSession;
import org.apache.maven.execution.MojoExecutionEvent;
import org.apache.maven.execution.MojoExecutionListener;
import org.apache.maven.plugin.MojoExecutionException;

@Named
@Singleton
public class BuildExtensionAComponent
    extends AbstractMavenLifecycleParticipant
    implements MojoExecutionListener
{
    public static final String FILE_PATH = "target/executions.txt";

    private final Set<String> executions = new TreeSet<String>();

    @Override
    public void afterSessionEnd( MavenSession session )
        throws MavenExecutionException
    {
        try
        {
            File file = new File( session.getTopLevelProject().getBasedir(), FILE_PATH );
            file.getParentFile().mkdirs();
            Writer w = new OutputStreamWriter( new FileOutputStream( file, false ), "UTF-8" );
            try
            {
                for (String execution : executions)
                {
                    w.write( execution + "\n" );
                }
            }
            finally
            {
                w.close();
            }
        }
        catch ( IOException e )
        {
            throw new RuntimeException( e.getMessage(), e );
        }

    }

    public void beforeMojoExecution( MojoExecutionEvent event )
        throws MojoExecutionException
    {
        executions.add( event.getExecution().toString() );
    }

    public void afterMojoExecutionSuccess( MojoExecutionEvent event )
        throws MojoExecutionException
    {
    }

    public void afterExecutionFailure( MojoExecutionEvent event )
    {
    }
}
