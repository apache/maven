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

import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

public class MojoDescriptor
    implements Cloneable
{
    public static final String SINGLE_PASS_EXEC_STRATEGY = "once-per-session";

    public static final String MULTI_PASS_EXEC_STRATEGY = "always";

    private static final String DEFAULT_LANGUAGE = "java";

    private String implementation;

    private String description;

    private String id;

    private List parameters;

    private Map parameterMap;

    private String instantiationStrategy = "singleton";

    private String executionStrategy = SINGLE_PASS_EXEC_STRATEGY;

    private String goal;

    private String phase;

    // ----------------------------------------------------------------------
    //
    // ----------------------------------------------------------------------

    private List prereqs;

    private boolean requiresDependencyResolution = false;

    private boolean requiresProject = true;

    private String language = DEFAULT_LANGUAGE;

    // ----------------------------------------------------------------------
    //
    // ----------------------------------------------------------------------

    public String getRole()
    {
        return "org.apache.maven.plugin.Plugin";
    }

    public String getImplementation()
    {
        return implementation;
    }

    public void setImplementation( String implementation )
    {
        this.implementation = implementation;
    }

    public String getDescription()
    {
        return description;
    }

    public void setDescription( String description )
    {
        this.description = description;
    }

    public String getInstantiationStrategy()
    {
        return instantiationStrategy;
    }

    public void setInstantiationStrategy( String instantiationStrategy )
    {
        this.instantiationStrategy = instantiationStrategy;
    }

    public String getLanguage()
    {
        return language;
    }

    public void setLanguage( String language )
    {
        this.language = language;
    }

    public String getId()
    {
        return id;
    }

    public void setId( String id )
    {
        this.id = id;
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

    public void setRequiresDependencyResolution( boolean requiresDependencyResolution )
    {
        this.requiresDependencyResolution = requiresDependencyResolution;
    }

    public boolean requiresDependencyResolution()
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

    public boolean requiresProject()
    {
        return requiresProject;
    }

    // ----------------------------------------------------------------------
    // Prereqs
    // ----------------------------------------------------------------------

    public List getPrereqs()
    {
        return prereqs;
    }

    public void setPrereqs( List prereqs )
    {
        this.prereqs = prereqs;
    }

    // ----------------------------------------------------------------------
    //
    // ----------------------------------------------------------------------

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

    public MojoDescriptor copy()
    {
        try
        {
            return (MojoDescriptor) this.clone();
        }
        catch ( Exception e )
        {
            // TODO: this needs better handling
        }

        return null;
    }
}
