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

/**
 *
 * 
 * @author <a href="mailto:jason@maven.org">Jason van Zyl</a>
 *
 * @version $Id$
 */
public class TestPlugin
    implements Plugin
{
    protected boolean executed;

    protected String name;

    protected String artifactId;

    protected String foo;

    public boolean hasExecuted()
    {
        return executed;
    }

    public String getName()
    {
        return name;
    }

    public String getArtifactId()
    {
        return artifactId;
    }

    public String getFoo()
    {
        return foo;
    }

    public void execute( PluginExecutionRequest request, PluginExecutionResponse response )
        throws Exception
    {
        name = (String) request.getParameter( "name" );

        artifactId = (String) request.getParameter( "artifactId" );

        foo = (String) request.getParameter( "foo" );

        executed = true;
    }
}
