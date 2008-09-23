package org.apache.maven.plugins;

/*
 * Copyright 2001-2005 The Apache Software Foundation.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

import org.apache.maven.artifact.manager.WagonManager;
import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.wagon.authentication.AuthenticationInfo;

/**
 * Goal which checks for a server entry of the specified serverId in the wagon manager.
 *
 * @goal test
 * @phase initialize
 */
public class MyMojo
    extends AbstractMojo
{
    
    /**
     * @component
     */
    private WagonManager wagonManager;
    
    /**
     * @parameter default-value="test"
     * @required
     */
    private String serverId;
    
    /**
     * @parameter default-value="testuser"
     * @required
     */
    private String username;
    
    /**
     * @parameter default-value="testtest"
     * @required
     */
    private String password;
    
    public void execute()
        throws MojoExecutionException
    {
        AuthenticationInfo authInfo = wagonManager.getAuthenticationInfo( serverId );
        
        if ( authInfo == null )
        {
            getLog().error( "Cannot find AuthenticationInfo for: " + serverId + "." );
            throw new MojoExecutionException( "Cannot find AuthenticationInfo for: " + serverId + "." );
        }
        
        if ( !username.equals( authInfo.getUserName() ) )
        {
            getLog().error( "Expected username: '" + username + "'; found: '" + authInfo.getUserName() + "'" );
            throw new MojoExecutionException( "Expected username: " + username + "; found: " + authInfo.getUserName() );
        }
        
        if ( !password.equals( authInfo.getPassword() ) )
        {
            getLog().error( "Expected password: '" + password + "'; found: '" + authInfo.getPassword() + "'" );
            throw new MojoExecutionException( "Expected password: " + password + "; found: " + authInfo.getPassword() );
        }
    }
}
