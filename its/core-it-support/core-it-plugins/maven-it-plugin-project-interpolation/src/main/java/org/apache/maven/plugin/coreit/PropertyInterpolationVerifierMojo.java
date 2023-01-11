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
 *  http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */

import org.apache.maven.model.Model;
import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.plugins.annotations.LifecyclePhase;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.Parameter;
import org.apache.maven.project.MavenProject;

import java.util.Enumeration;
import java.util.Properties;

/**
  */
@Mojo( name = "verify-property", defaultPhase = LifecyclePhase.VALIDATE )
public class PropertyInterpolationVerifierMojo
    extends AbstractMojo
{

    /**
     * The current Maven project.
     */
    @Parameter( defaultValue = "${project}" )
    private MavenProject project;

    /**
     * The properties.
     */
    @Parameter( property = "clsldr.pluginClassLoaderOutput" )
    private Properties properties;

    public void execute()
        throws MojoExecutionException, MojoFailureException
    {
        Model model = project.getModel();
        if ( properties == null )
        {
            return;
        }

        Enumeration e = properties.propertyNames();
        while ( e.hasMoreElements() )
        {
            String name = (String) e.nextElement();
            String value = properties.getProperty( name );
            if ( !value.equals( model.getProperties().getProperty( name ) ) )
            {
                throw new MojoExecutionException( "Properties do not match: Name = " + name + ", Value = " + value );
            }

            if ( value.contains( "${" ) )
            {
                throw new MojoExecutionException( "Unresolved value: Name = " + name + ", Value = " + value );
            }

            getLog().info( "Property match: Name = " + name + ", Value = " + value );
        }
    }
}
