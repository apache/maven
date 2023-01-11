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

import org.apache.maven.artifact.manager.WagonManager;
import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugins.annotations.Component;
import org.apache.maven.plugins.annotations.LifecyclePhase;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.wagon.Wagon;
import org.apache.maven.wagon.providers.file.FileWagon;
import org.apache.maven.wagon.providers.ssh.jsch.ScpWagon;

/**
 */
@Mojo( name = "use-wagon", defaultPhase = LifecyclePhase.VALIDATE )
public class UsesWagonMojo
    extends AbstractMojo
{

    /**
     */
    @Component
    private WagonManager wagonManager;

    public void execute()
        throws MojoExecutionException
    {
        Wagon fileWagon;
        try
        {
            getLog().info( "[MAVEN-CORE-IT-LOG] Looking up wagon for protocol file" );
            fileWagon = wagonManager.getWagon( "file" );
        }
        catch ( Exception e )
        {
            throw new MojoExecutionException( e.getMessage(), e );
        }
        try
        {
            FileWagon theWagon = (FileWagon) fileWagon;
        }
        catch ( ClassCastException e )
        {
            getLog().error( "", e );
            getLog().error( "Plugin Class Loaded by " + FileWagon.class.getClassLoader() );
            getLog().error( "Wagon Class Loaded by " + fileWagon.getClass().getClassLoader() );

            throw e;
        }

        Wagon scpWagon;
        try
        {
            getLog().info( "[MAVEN-CORE-IT-LOG] Looking up wagon for protocol scp" );
            scpWagon = wagonManager.getWagon( "scp" );
        }
        catch ( Exception e )
        {
            throw new MojoExecutionException( e.getMessage(), e );
        }
        try
        {
            ScpWagon theWagon = (ScpWagon) scpWagon;
        }
        catch ( ClassCastException e )
        {
            getLog().error( "", e );
            getLog().error( "Plugin Class Loaded by " + ScpWagon.class.getClassLoader() );
            getLog().error( "Wagon Class Loaded by " + scpWagon.getClass().getClassLoader() );

            throw e;
        }
    }

}
