package org.apache.maven.plugin;

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

/**
 * A failure or exception occuring during the execution of a plugin.
 *
 * @author Brett Porter
 * @version $Id$
 */
public class PluginExecutionException extends Exception
{
    private Object source;

    private String longMessage;

    public PluginExecutionException( Object source, String shortMessage, String longMessage )
    {
        super( shortMessage );
        this.source = source;
        this.longMessage = longMessage;
    }

    public PluginExecutionException( String message, Exception cause )
    {
        super( message, cause );
    }

    public PluginExecutionException( String message )
    {
        super( message );
    }

    public String getLongMessage()
    {
        return longMessage;
    }
}
