package org.apache.maven.plugins.projecthelp;

import org.apache.maven.model.Profile;
import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.logging.Log;

import java.util.Iterator;
import java.util.List;

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

/** Lists the profiles which are currently active for this build.
 * 
 * @goal active-profiles
 */
public class ActiveProfilesPlugin extends AbstractMojo
{
    
    /**
     * @parameter expression="${project.activeProfiles}"
     * @required
     * @readonly
     */
    private List profiles;

    public void execute()
        throws MojoExecutionException
    {
        StringBuffer message = new StringBuffer();
        
        message.append( "\n" );
        
        if( profiles == null || profiles.isEmpty() )
        {
            message.append( "There are no active profiles." );
        }
        else
        {
            message.append( "The following profiles are active:\n\n" );
            
            for ( Iterator it = profiles.iterator(); it.hasNext(); )
            {
                Profile profile = (Profile) it.next();
                
                message.append( "\n - " )
                       .append( profile.getId() )
                       .append(" (source: " )
                       .append( profile.getSource() ).append( ")" );
            }
            
        }
        
        message.append( "\n\n" );
        
        Log log = getLog();
        
        log.info( message );
        
    }

}
