package org.apache.maven.plugin;

/*
 * Copyright 2001-2004 The Apache Software Foundation.
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

import java.util.Map;
import java.util.HashMap;

/**
 * Adapt a maven2 plugin for use as a bean with setters that can be used
 * within Jelly and Ant.
 *
 * @author <a href="mailto:jason@maven.org">Jason van Zyl</a>
 * @version $Id$
 */
public class BeanPluginAdapter
{
    private Map parameters;

    private Plugin plugin;

    public BeanPluginAdapter( Plugin plugin )
    {
        this.plugin = plugin;

        parameters = new HashMap();
    }

    // ----------------------------------------------------------------------
    //
    // ----------------------------------------------------------------------

    public void execute()
        throws Exception
    {
        PluginExecutionRequest request = new PluginExecutionRequest( parameters );

        PluginExecutionResponse response = new PluginExecutionResponse();

        plugin.execute( request, response );
    }

    // ----------------------------------------------------------------------
    //
    // ----------------------------------------------------------------------

    protected void addParameter( String key, Object value )
    {
        parameters.put( key, value );
    }

    protected Plugin getPlugin()
    {
        return plugin;
    }
}
