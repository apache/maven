package org.apache.maven.plugin.descriptor;

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

import org.apache.maven.plugin.Mojo;
import org.codehaus.plexus.component.repository.ComponentDescriptor;
import org.codehaus.plexus.configuration.PlexusConfiguration;
import org.codehaus.plexus.configuration.xml.XmlPlexusConfiguration;

import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

/**
 * The bean containing the Mojo descriptor.
 * <br/>
 * For more information about the usage tag, have a look to:
 * <a href="http://maven.apache.org/developers/mojo-api-specification.html">http://maven.apache.org/developers/mojo-api-specification.html</a>
 *
 * @todo is there a need for the delegation of MavenMojoDescriptor to this?
 * Why not just extend ComponentDescriptor here?
 * @version $Id$
 */
public class MojoDescriptor
    extends ComponentDescriptor
    implements Cloneable
{
    /** The Plexus component type */
    public static String MAVEN_PLUGIN = "maven-plugin";

    /** "once-per-session" execution strategy */
    public static final String SINGLE_PASS_EXEC_STRATEGY = "once-per-session";

    /** "always" execution strategy */
    public static final String MULTI_PASS_EXEC_STRATEGY = "always";

    private static final String DEFAULT_INSTANTIATION_STRATEGY = "per-lookup";

    private static final String DEFAULT_LANGUAGE = "java";

    private List parameters;

    private Map parameterMap;

    /** By default, the execution strategy is "once-per-session" */
    private String executionStrategy = SINGLE_PASS_EXEC_STRATEGY;

    /** The goal name of the Mojo */
    private String goal;

    /** Reference the binded phase name of the Mojo */
    private String phase;

    /** Specify the version when the Mojo was added to the API. Similar to Javadoc since. */
    private String since;

    /** Reference the invocation phase of the Mojo */
    private String executePhase;

    /** Reference the invocation goal of the Mojo */
    private String executeGoal;

    /** Reference the invocation lifecycle of the Mojo */
    private String executeLifecycle;

    /** Specify the version when the Mojo was deprecated to the API. Similar to Javadoc deprecated. */
    private String deprecated;

    /** By default, no need to aggregate the Maven project and its child modules */
    private boolean aggregator = false;

    // ----------------------------------------------------------------------
    //
    // ----------------------------------------------------------------------

    /** Specify the required dependencies in a specified scope */
    private String dependencyResolutionRequired = null;

    /**  By default, the Mojo needs a Maven project to be executed */
    private boolean projectRequired = true;

    /**  By default, the Mojo is online */
    private boolean onlineRequired = false;

    /**  Plugin configuration */
    private PlexusConfiguration mojoConfiguration;

    /**  Plugin descriptor */
    private PluginDescriptor pluginDescriptor;

    /**  By default, the Mojo is herited */
    private boolean inheritedByDefault = true;

    /**  By default, the Mojo could not be invoke directly */
    private boolean directInvocationOnly = false;

    /**  By default, the Mojo don't need reports to run */
    private boolean requiresReports = false;

    /**
     * Default constructor.
     */
    public MojoDescriptor()
    {
        setInstantiationStrategy( DEFAULT_INSTANTIATION_STRATEGY );
        setComponentFactory( DEFAULT_LANGUAGE );
    }

    // ----------------------------------------------------------------------
    //
    // ----------------------------------------------------------------------

    /**
     * @return the language of this Mojo, i.e. <code>java</code>
     */
    public String getLanguage()
    {
        return getComponentFactory();
    }

    /**
     * @param language the new language
     */
    public void setLanguage( String language )
    {
        setComponentFactory( language );
    }

    /**
     * @return <code>true</code> if the Mojo is deprecated, <code>false</code> otherwise.
     */
    public String getDeprecated()
    {
        return deprecated;
    }

    /**
     * @param deprecated <code>true</code> to deprecate the Mojo, <code>false</code> otherwise.
     */
    public void setDeprecated( String deprecated )
    {
        this.deprecated = deprecated;
    }

    /**
     * @return the list of parameters
     */
    public List getParameters()
    {
        return parameters;
    }

    /**
     * @param parameters the new list of parameters
     * @throws DuplicateParameterException if any
     */
    public void setParameters( List parameters )
        throws DuplicateParameterException
    {
        for ( Iterator it = parameters.iterator(); it.hasNext(); )
        {
            Parameter parameter = (Parameter) it.next();
            addParameter( parameter );
        }
    }

    /**
     * @param parameter add a new parameter
     * @throws DuplicateParameterException if any
     */
    public void addParameter( Parameter parameter )
        throws DuplicateParameterException
    {
        if ( parameters != null && parameters.contains( parameter ) )
        {
            throw new DuplicateParameterException( parameter.getName() +
                " has been declared multiple times in mojo with goal: " + getGoal() + " (implementation: " +
                getImplementation() + ")" );
        }

            if ( parameters == null )
            {
                parameters = new LinkedList();
            }

            parameters.add( parameter );
        }

    /**
     * @return the list parameters as a Map
     */
    public Map getParameterMap()
    {
        if ( parameterMap == null )
        {
            parameterMap = new HashMap();

            if ( parameters != null )
            {
                for ( Iterator iterator = parameters.iterator(); iterator.hasNext(); )
                {
                    Parameter pd = (Parameter) iterator.next();

                    parameterMap.put( pd.getName(), pd );
                }
            }
        }

        return parameterMap;
    }

    // ----------------------------------------------------------------------
    // Dependency requirement
    // ----------------------------------------------------------------------

    /**
     * @param requiresDependencyResolution the new required dependencies in a specified scope
     */
    public void setDependencyResolutionRequired( String requiresDependencyResolution )
    {
        this.dependencyResolutionRequired = requiresDependencyResolution;
    }

    /**
     * @return the required dependencies in a specified scope
     * @TODO the name is not intelligible
     */
    public String isDependencyResolutionRequired()
    {
        return dependencyResolutionRequired;
    }

    // ----------------------------------------------------------------------
    // Project requirement
    // ----------------------------------------------------------------------

    /**
     * @param requiresProject <code>true</code> if the Mojo needs a Maven project to be executed, <code>false</code> otherwise.
     */
    public void setProjectRequired( boolean requiresProject )
    {
        this.projectRequired = requiresProject;
    }

    /**
     * @return <code>true</code> if the Mojo needs a Maven project to be executed, <code>false</code> otherwise.
     */
    public boolean isProjectRequired()
    {
        return projectRequired;
    }

    // ----------------------------------------------------------------------
    // Online vs. Offline requirement
    // ----------------------------------------------------------------------

    /**
     * @param requiresOnline <code>true</code> if the Mojo is online, <code>false</code> otherwise.
     */
    public void setOnlineRequired( boolean requiresOnline )
    {
        this.onlineRequired = requiresOnline;
    }

    /**
     * @return <code>true</code> if the Mojo is online, <code>false</code> otherwise.
     */
    // blech! this isn't even intelligible as a method name. provided for
    // consistency...
    public boolean isOnlineRequired()
    {
        return onlineRequired;
    }

    /**
     * @return <code>true</code> if the Mojo is online, <code>false</code> otherwise.
     */
    // more english-friendly method...keep the code clean! :)
    public boolean requiresOnline()
    {
        return onlineRequired;
    }

    /**
     * @return the binded phase name of the Mojo
     */
    public String getPhase()
    {
        return phase;
    }

    /**
     * @param phase the new binded phase name of the Mojo
     */
    public void setPhase( String phase )
    {
        this.phase = phase;
    }

    /**
     * @return the version when the Mojo was added to the API
     */
    public String getSince()
    {
        return since;
    }

    /**
     * @param since the new version when the Mojo was added to the API
     */
    public void setSince( String since )
    {
        this.since = since;
    }

    /**
     * @return The goal name of the Mojo
     */
    public String getGoal()
    {
        return goal;
    }

    /**
     * @param goal The new goal name of the Mojo
     */
    public void setGoal( String goal )
    {
        this.goal = goal;
    }

    /**
     * @return the invocation phase of the Mojo
     */
    public String getExecutePhase()
    {
        return executePhase;
    }

    /**
     * @param executePhase the new invocation phase of the Mojo
     */
    public void setExecutePhase( String executePhase )
    {
        this.executePhase = executePhase;
    }

    /**
     * @return <code>true</code> if the Mojo uses <code>always</code> for the <code>executionStrategy</code>
     */
    public boolean alwaysExecute()
    {
        return MULTI_PASS_EXEC_STRATEGY.equals( executionStrategy );
    }

    /**
     * @return the execution strategy
     */
    public String getExecutionStrategy()
    {
        return executionStrategy;
    }

    /**
     * @param executionStrategy the new execution strategy
     */
    public void setExecutionStrategy( String executionStrategy )
    {
        this.executionStrategy = executionStrategy;
    }

    /**
     * @return the mojo configuration
     */
    public PlexusConfiguration getMojoConfiguration()
    {
        if ( mojoConfiguration == null )
        {
            mojoConfiguration = new XmlPlexusConfiguration( "configuration" );
        }
        return mojoConfiguration;
    }

    /**
     * @param mojoConfiguration a new mojo configuration
     */
    public void setMojoConfiguration( PlexusConfiguration mojoConfiguration )
    {
        this.mojoConfiguration = mojoConfiguration;
    }

    /** {@inheritDoc} */
    public String getRole()
    {
        return Mojo.ROLE;
    }

    /** {@inheritDoc} */
    public String getRoleHint()
    {
        return getId();
    }

    /**
     * @return the id of the mojo, based on the goal name
     */
    public String getId()
    {
        return getPluginDescriptor().getId() + ":" + getGoal();
    }

    /**
     * @return the full goal name
     * @see PluginDescriptor#getGoalPrefix()
     * @see #getGoal()
     */
    public String getFullGoalName()
    {
        return getPluginDescriptor().getGoalPrefix() + ":" + getGoal();
    }

    /** {@inheritDoc} */
    public String getComponentType()
    {
        return MAVEN_PLUGIN;
    }

    /**
     * @return the plugin descriptor
     */
    public PluginDescriptor getPluginDescriptor()
    {
        return pluginDescriptor;
    }

    /**
     * @param pluginDescriptor the new plugin descriptor
     */
    public void setPluginDescriptor( PluginDescriptor pluginDescriptor )
    {
        this.pluginDescriptor = pluginDescriptor;
    }

    /**
     * @return <code>true</code> if the Mojo is herited, <code>false</code> otherwise.
     */
    public boolean isInheritedByDefault()
    {
        return inheritedByDefault;
    }

    /**
     * @param inheritedByDefault <code>true</code> if the Mojo is herited, <code>false</code> otherwise.
     */
    public void setInheritedByDefault( boolean inheritedByDefault )
    {
        this.inheritedByDefault = inheritedByDefault;
    }

    /** {@inheritDoc} */
    public boolean equals( Object object )
    {
        if ( this == object )
        {
            return true;
        }

        if ( object instanceof MojoDescriptor )
        {
            MojoDescriptor other = (MojoDescriptor) object;

            if ( !compareObjects( getPluginDescriptor(), other.getPluginDescriptor() ) )
            {
                return false;
            }

            if ( !compareObjects( getGoal(), other.getGoal() ) )
            {
                return false;
            }

            return true;
        }

        return false;
    }

    private boolean compareObjects( Object first, Object second )
    {
        if ( ( first == null && second != null ) || ( first != null && second == null ) )
        {
            return false;
        }

        if ( !first.equals( second ) )
        {
            return false;
        }

        return true;
    }

    /** {@inheritDoc} */
    public int hashCode()
    {
        int result = 1;

        String goal = getGoal();

        if ( goal != null )
        {
            result += goal.hashCode();
        }

        PluginDescriptor pd = getPluginDescriptor();

        if ( pd != null )
        {
            result -= pd.hashCode();
        }

        return result;
    }

    /**
     * @return the invocation lifecycle of the Mojo
     */
    public String getExecuteLifecycle()
    {
        return executeLifecycle;
    }

    /**
     * @param executeLifecycle the new invocation lifecycle of the Mojo
     */
    public void setExecuteLifecycle( String executeLifecycle )
    {
        this.executeLifecycle = executeLifecycle;
    }

    /**
     * @param aggregator <code>true</code> if the Mojo uses the Maven project and its child modules, <code>false</code> otherwise.
     */
    public void setAggregator( boolean aggregator )
    {
        this.aggregator = aggregator;
    }

    /**
     * @return <code>true</code> if the Mojo uses the Maven project and its child modules, <code>false</code> otherwise.
     */
    public boolean isAggregator()
    {
        return aggregator;
    }

    /**
     * @return <code>true</code> if the Mojo could not be invoke directly, <code>false</code> otherwise.
     */
    public boolean isDirectInvocationOnly()
    {
        return directInvocationOnly;
    }

    /**
     * @param directInvocationOnly <code>true</code> if the Mojo could not be invoke directly, <code>false</code> otherwise.
     */
    public void setDirectInvocationOnly( boolean directInvocationOnly )
    {
        this.directInvocationOnly = directInvocationOnly;
    }

    /**
     * @return <code>true</code> if the Mojo needs reports to run, <code>false</code> otherwise.
     */
    public boolean isRequiresReports()
    {
        return requiresReports;
    }

    /**
     * @param requiresReports <code>true</code> if the Mojo needs reports to run, <code>false</code> otherwise.
     */
    public void setRequiresReports( boolean requiresReports )
    {
        this.requiresReports = requiresReports;
    }

    /**
     * @param executeGoal the new invocation goal of the Mojo
     */
    public void setExecuteGoal( String executeGoal )
    {
        this.executeGoal = executeGoal;
    }

    /**
     * @return the invocation goal of the Mojo
     */
    public String getExecuteGoal()
    {
        return executeGoal;
    }
}
