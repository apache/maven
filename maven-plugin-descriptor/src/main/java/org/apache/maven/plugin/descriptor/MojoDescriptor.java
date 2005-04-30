package org.apache.maven.plugin.descriptor;

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

import org.apache.maven.plugin.Mojo;
import org.codehaus.plexus.component.repository.ComponentDescriptor;
import org.codehaus.plexus.configuration.PlexusConfiguration;
import org.codehaus.plexus.configuration.xml.XmlPlexusConfiguration;

import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

/**
 * The bean containing the mojo descriptor.
 *
 * @todo is there a need for the delegation of MavenMojoDescriptor to this? Why not just extend ComponentDescriptor here?
 */
public class MojoDescriptor
    extends ComponentDescriptor
    implements Cloneable
{
    // TODO: share with type handler
    public static String MAVEN_PLUGIN = "maven-plugin";

    public static final String SINGLE_PASS_EXEC_STRATEGY = "once-per-session";

    public static final String MULTI_PASS_EXEC_STRATEGY = "always";

    private static final String DEFAULT_INSTANTIATION_STRATEGY = "per-lookup";

    private static final String DEFAULT_LANGUAGE = "java";

    private String id;

    private List parameters;

    private Map parameterMap;

    private String executionStrategy = SINGLE_PASS_EXEC_STRATEGY;

    private String goal;

    private String phase;

    private String executePhase;

    private String deprecated;

    // ----------------------------------------------------------------------
    //
    // ----------------------------------------------------------------------

    private String requiresDependencyResolution = null;

    private boolean requiresProject = true;

    private boolean requiresOnline = false;

    private PlexusConfiguration mojoConfiguration;

    public MojoDescriptor()
    {
        setInstantiationStrategy( DEFAULT_INSTANTIATION_STRATEGY );
        setComponentFactory( DEFAULT_LANGUAGE );
    }

    // ----------------------------------------------------------------------
    //
    // ----------------------------------------------------------------------

    public String getLanguage()
    {
        return getComponentFactory();
    }

    public void setLanguage( String language )
    {
        setComponentFactory( language );
    }

    public String getId()
    {
        return id;
    }

    public void setId( String id )
    {
        this.id = id;
    }

    public String getDeprecated()
    {
        return deprecated;
    }

    public void setDeprecated( String deprecated )
    {
        this.deprecated = deprecated;
    }

    public List getParameters()
    {
        return parameters;
    }

    public void setParameters( List parameters )
    {
        this.parameters = parameters;
    }

    public Map getParameterMap()
    {
        if ( parameterMap == null )
        {
            parameterMap = new HashMap();

            for ( Iterator iterator = parameters.iterator(); iterator.hasNext(); )
            {
                Parameter pd = (Parameter) iterator.next();

                parameterMap.put( pd.getName(), pd );
            }
        }

        return parameterMap;
    }

    // ----------------------------------------------------------------------
    // Dependency requirement
    // ----------------------------------------------------------------------

    public void setRequiresDependencyResolution( String requiresDependencyResolution )
    {
        this.requiresDependencyResolution = requiresDependencyResolution;
    }

    public String getRequiresDependencyResolution()
    {
        return requiresDependencyResolution;
    }

    // ----------------------------------------------------------------------
    // Project requirement
    // ----------------------------------------------------------------------

    public void setRequiresProject( boolean requiresProject )
    {
        this.requiresProject = requiresProject;
    }

    public boolean isRequiresProject()
    {
        return requiresProject;
    }

    // ----------------------------------------------------------------------
    // Online vs. Offline requirement
    // ----------------------------------------------------------------------

    public void setRequiresOnline( boolean requiresOnline )
    {
        this.requiresOnline = requiresOnline;
    }

    // blech! this isn't even intelligible as a method name. provided for
    // consistency...
    public boolean isRequiresOnline()
    {
        return requiresOnline;
    }

    // more english-friendly method...keep the code clean! :)
    public boolean requiresOnline()
    {
        return requiresOnline;
    }

    public String getPhase()
    {
        return phase;
    }

    public void setPhase( String phase )
    {
        this.phase = phase;
    }

    public String getGoal()
    {
        return goal;
    }

    public void setGoal( String goal )
    {
        this.goal = goal;
    }

    public String getExecutePhase()
    {
        return executePhase;
    }

    public void setExecutePhase( String executePhase )
    {
        this.executePhase = executePhase;
    }

    public boolean alwaysExecute()
    {
        return MULTI_PASS_EXEC_STRATEGY.equals( executionStrategy );
    }

    public String getExecutionStrategy()
    {
        return executionStrategy;
    }

    public void setExecutionStrategy( String executionStrategy )
    {
        this.executionStrategy = executionStrategy;
    }

    public PlexusConfiguration getMojoConfiguration()
    {
        if ( mojoConfiguration == null )
        {
            mojoConfiguration = new XmlPlexusConfiguration( "configuration" );
        }
        return mojoConfiguration;
    }

    public void setMojoConfiguration( PlexusConfiguration mojoConfiguration )
    {
        this.mojoConfiguration = mojoConfiguration;
    }

    public String getRole()
    {
        return Mojo.ROLE;
    }

    public String getRoleHint()
    {
        return getId();
    }

    public String getComponentType()
    {
        return MAVEN_PLUGIN;
    }

}
