package org.apache.maven.script.ant;

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

import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugin.ContextEnabled;
import org.apache.maven.plugin.MojoExecutionException;
import org.codehaus.plexus.component.MapOrientedComponent;
import org.codehaus.plexus.component.configurator.ComponentConfigurationException;
import org.codehaus.plexus.component.factory.ant.AntComponentExecutionException;
import org.codehaus.plexus.component.factory.ant.AntScriptInvoker;
import org.codehaus.plexus.component.repository.ComponentRequirement;

import java.util.Map;


public class AntMojoWrapper
    extends AbstractMojo
    implements ContextEnabled, MapOrientedComponent
{

    private Map pluginContext;
    private final AntScriptInvoker scriptInvoker;

    public AntMojoWrapper( AntScriptInvoker scriptInvoker )
    {
        this.scriptInvoker = scriptInvoker;
    }

    public void execute()
        throws MojoExecutionException
    {
        try
        {
            scriptInvoker.invoke();
        }
        catch ( AntComponentExecutionException e )
        {
            throw new MojoExecutionException( "Failed to execute: " + e.getMessage(), e );
        }
    }

    public void setPluginContext( Map pluginContext )
    {
        this.pluginContext = pluginContext;
    }

    public Map getPluginContext()
    {
        return pluginContext;
    }

    public void addComponentRequirement( ComponentRequirement requirementDescriptor, Object requirementValue )
        throws ComponentConfigurationException
    {
        scriptInvoker.addComponentRequirement( requirementDescriptor, requirementValue );
    }

    public void setComponentConfiguration( Map componentConfiguration )
        throws ComponentConfigurationException
    {
        scriptInvoker.setComponentConfiguration( componentConfiguration );
    }

}
