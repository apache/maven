package org.apache.plugins.csharp.helpers;

/*
 * Copyright 2005 The Apache Software Foundation.
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

import org.apache.maven.plugin.logging.Log;
import org.apache.tools.ant.BuildEvent;
import org.apache.tools.ant.BuildListener;
import org.apache.tools.ant.Project;


/**
 * @author <a href="mailto:gdodinet@karmicsoft.com">Gilles Dodinet</a>
 * @version $Id$
 */
public final class AntBuildListener
    implements BuildListener
{
    private Log log;

    public AntBuildListener()
    {
    }

    public AntBuildListener( Log log )
    {
        this.log = log;
    }

    public void buildFinished( BuildEvent arg0 )
    {
    }

    public void buildStarted( BuildEvent arg0 )
    {
    }

    public void targetStarted( BuildEvent arg0 )
    {
    }

    public void targetFinished( BuildEvent arg0 )
    {
    }

    public void taskStarted( BuildEvent arg0 )
    {
    }

    public void taskFinished( BuildEvent arg0 )
    {
    }

    public void messageLogged( BuildEvent e )
    {
        if ( log == null )
        {
            System.out.println( e.getMessage() );
            return;
        }
        switch ( e.getPriority() )
        {
            case Project.MSG_DEBUG:
                log.debug( e.getMessage() );
                break;
            case Project.MSG_INFO :
                log.info( e.getMessage() );
                break;
            case Project.MSG_WARN :
                log.warn( e.getMessage() );
                break;
            case Project.MSG_ERR  :
                log.error( e.getMessage() );
                break;
            default :
                break;
        }
    }
}
