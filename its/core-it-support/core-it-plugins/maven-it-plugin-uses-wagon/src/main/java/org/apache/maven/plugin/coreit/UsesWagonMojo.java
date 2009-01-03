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
import org.apache.maven.wagon.Wagon;
import org.apache.maven.wagon.providers.ssh.jsch.ScpWagon;

/**
 * @goal use-wagon
 * @phase validate
 */
public class UsesWagonMojo
    extends AbstractMojo
{
    /**
     * @component
     */
    private WagonManager wagonManager;
    
    public void execute()
        throws MojoExecutionException
    {
        try
        {
            getLog().info( "[MAVEN-CORE-IT-LOG] Looking up wagon for protocol scp" );
            Wagon wagon = wagonManager.getWagon( "scp" );

            ScpWagon myWagon = (ScpWagon) wagon;
            getLog().info( "[MAVEN-CORE-IT-LOG] Looked up and successfully casted scp wagon: " + myWagon );
        }
        catch( Exception e )
        {
            throw new MojoExecutionException( e.getMessage(), e );
        }
    }
}
