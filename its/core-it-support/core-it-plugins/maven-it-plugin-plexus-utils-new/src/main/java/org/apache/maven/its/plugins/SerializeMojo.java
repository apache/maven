package org.apache.maven.its.plugins;

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

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.io.Writer;

import org.apache.commons.io.IOUtils;
import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.plugins.annotations.LifecyclePhase;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.Parameter;
import org.codehaus.plexus.util.xml.Xpp3Dom;
import org.codehaus.plexus.util.xml.pull.MXSerializer;
import org.codehaus.plexus.util.xml.pull.XmlSerializer;

/**
  */
@Mojo( name = "serialize", defaultPhase = LifecyclePhase.VALIDATE )
public class SerializeMojo
    extends AbstractMojo
{

    @Parameter( defaultValue = "${project.build.directory}/serialized.xml" )
    private File file;

    public void execute()
        throws MojoExecutionException, MojoFailureException
    {
        Writer writer = null;
        XmlSerializer s = new MXSerializer();
        try
        {
            file.getParentFile().mkdirs();
            writer = new OutputStreamWriter( new FileOutputStream( file ), "UTF-8" );
            s.setOutput( writer );

            Xpp3Dom dom = new Xpp3Dom( "root" );

            dom.writeToSerializer( "", s );
        }
        catch ( IOException e )
        {
            throw new MojoExecutionException( e.getMessage(), e );
        }
        finally
        {
            IOUtils.closeQuietly( writer );
        }
    }
}
