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

import junit.framework.TestCase;

import java.util.Map;

/**
 * @author <a href="mailto:jason@maven.org">Jason van Zyl</a>
 * @version $Id$
 */
public abstract class PluginTestCase
    extends TestCase
{
    protected Plugin plugin;

    protected PluginExecutionRequest request;

    protected PluginExecutionResponse response;

    protected String basedir;

    // ----------------------------------------------------------------------
    //
    // ----------------------------------------------------------------------

    protected PluginTestCase( String s )
    {
        super( s );
    }

    protected void setUp()
        throws Exception
    {
        basedir = System.getProperty( "basedir" );
    }

    // ----------------------------------------------------------------------
    //
    // ----------------------------------------------------------------------

    protected void setupPlugin()
        throws Exception
    {
        String mojoClassName = getClass().getName();

        mojoClassName = mojoClassName.substring( 0, mojoClassName.length() - 4 );

        try
        {
            Class mojoClass = Thread.currentThread().getContextClassLoader().loadClass( mojoClassName );

            plugin = (Plugin) mojoClass.newInstance();
        }
        catch ( Exception e )
        {
            throw new Exception(
                "Cannot find " + mojoClassName + "! Make sure your test case is named in the form ${mojoClassName}Test " +
                "or override the setupPlugin() method to instantiate the mojo yourself." );
        }
    }

    protected abstract Map getTestParameters()
        throws Exception;

    protected abstract void validatePluginExecution()
        throws Exception;

    // ----------------------------------------------------------------------
    //
    // ----------------------------------------------------------------------

    public void testPlugin()
        throws Exception
    {
        setupPlugin();

        request = new PluginExecutionRequest( getTestParameters() );

        response = new PluginExecutionResponse();

        plugin.execute( request, response );

        validatePluginExecution();
    }
}
