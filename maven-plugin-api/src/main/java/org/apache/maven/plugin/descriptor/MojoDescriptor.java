/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */
package org.apache.maven.plugin.descriptor;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

import org.apache.maven.plugin.Mojo;
import org.codehaus.plexus.component.repository.ComponentDescriptor;
import org.codehaus.plexus.configuration.PlexusConfiguration;
import org.codehaus.plexus.configuration.xml.XmlPlexusConfiguration;

/**
 * The bean containing the Mojo descriptor.<br>
 * For more information about the usage tag, have a look to:
 * <a href="https://maven.apache.org/developers/mojo-api-specification.html">
 * https://maven.apache.org/developers/mojo-api-specification.html</a>
 *
 * TODO is there a need for the delegation of MavenMojoDescriptor to this?
 * Why not just extend ComponentDescriptor here?
 */
public class MojoDescriptor extends ComponentDescriptor<Mojo> implements Cloneable {
    /** The Plexus component type */
    public static final String MAVEN_PLUGIN = "maven-plugin";

    /** "once-per-session" execution strategy */
    public static final String SINGLE_PASS_EXEC_STRATEGY = "once-per-session";

    /** "always" execution strategy */
    public static final String MULTI_PASS_EXEC_STRATEGY = "always";

    private static final String DEFAULT_INSTANTIATION_STRATEGY = "per-lookup";

    private static final String DEFAULT_LANGUAGE = "java";

    private final ArrayList<Parameter> parameters;

    /** By default, the execution strategy is "once-per-session" */
    private String executionStrategy = SINGLE_PASS_EXEC_STRATEGY;

    /**
     * The goal name for the Mojo, that users will reference from the command line to execute the Mojo directly, or
     * inside a POM in order to provide Mojo-specific configuration.
     */
    private String goal;

    /**
     * Defines a default phase to bind a mojo execution to if the user does not explicitly set a phase in the POM.
     * <i>Note:</i> This will not automagically make a mojo run when the plugin declaration is added to the POM. It
     * merely enables the user to omit the <code>&lt;phase&gt;</code> element from the surrounding
     * <code>&lt;execution&gt;</code> element.
     */
    private String phase;

    /** Specify the version when the Mojo was added to the API. Similar to Javadoc since. */
    private String since;

    /** Reference the invocation phase of the Mojo. */
    private String executePhase;

    /** Reference the invocation goal of the Mojo. */
    private String executeGoal;

    /** Reference the invocation lifecycle of the Mojo. */
    private String executeLifecycle;

    /**
     * Description with reason of Mojo deprecation. Similar to Javadoc {@code @deprecated}.
     * This will trigger a warning when a user tries to use a Mojo marked as deprecated.
     */
    private String deprecated;

    /**
     * Flags this Mojo to run it in a multi-module way, i.e. aggregate the build with the set of projects listed as
     * modules. By default, no need to aggregate the Maven project and its child modules
     */
    private boolean aggregator = false;

    // ----------------------------------------------------------------------
    //
    // ----------------------------------------------------------------------

    /** Specify the required dependencies in a specified scope */
    private String dependencyResolutionRequired = null;

    /**
     * The scope of (transitive) dependencies that should be collected but not resolved.
     * @since 3.0-alpha-3
     */
    private String dependencyCollectionRequired;

    /**  By default, the Mojo needs a Maven project to be executed */
    private boolean projectRequired = true;

    /**  By default, the Mojo is assumed to work offline as well */
    private boolean onlineRequired = false;

    /**  Plugin configuration */
    private PlexusConfiguration mojoConfiguration;

    /**  Plugin descriptor */
    private PluginDescriptor pluginDescriptor;

    /**  By default, the Mojo is inherited */
    private boolean inheritedByDefault = true;

    /**  By default, the Mojo cannot be invoked directly */
    private boolean directInvocationOnly = false;

    /**  By default, the Mojo don't need reports to run */
    private boolean requiresReports = false;

    /**
     * By default, mojos are not threadsafe
     * @since 3.0-beta-2
     */
    private boolean threadSafe = false;

    /**
     * Default constructor.
     */
    public MojoDescriptor() {
        this.parameters = new ArrayList<>();
        setInstantiationStrategy(DEFAULT_INSTANTIATION_STRATEGY);
        setComponentFactory(DEFAULT_LANGUAGE);
    }

    // ----------------------------------------------------------------------
    //
    // ----------------------------------------------------------------------

    /**
     * @return the language of this Mojo, i.e. <code>java</code>
     */
    public String getLanguage() {
        return getComponentFactory();
    }

    /**
     * @param language the new language
     */
    public void setLanguage(String language) {
        setComponentFactory(language);
    }

    /**
     * @return Description with reason of a Mojo deprecation.
     */
    public String getDeprecated() {
        return deprecated;
    }

    /**
     * @param deprecated Description with reason of a Mojo deprecation.
     */
    public void setDeprecated(String deprecated) {
        this.deprecated = deprecated;
    }

    /**
     * @return the list of parameters copy. Any change to returned list is NOT reflected on this instance. To add
     * parameters, use {@link #addParameter(Parameter)} method.
     */
    public List<Parameter> getParameters() {
        return new ArrayList<>(parameters);
    }

    /**
     * @param parameters the new list of parameters
     * @throws DuplicateParameterException if any
     */
    public void setParameters(List<Parameter> parameters) throws DuplicateParameterException {
        this.parameters.clear();
        for (Parameter parameter : parameters) {
            addParameter(parameter);
        }
    }

    /**
     * @param parameter add a new parameter
     * @throws DuplicateParameterException if any
     */
    public void addParameter(Parameter parameter) throws DuplicateParameterException {
        if (parameters.contains(parameter)) {
            throw new DuplicateParameterException(parameter.getName()
                    + " has been declared multiple times in mojo with goal: " + getGoal() + " (implementation: "
                    + getImplementation() + ")");
        }

        parameters.add(parameter);
    }

    /**
     * @return the list parameters as a Map (keyed by {@link Parameter#getName()}) that is built from
     * {@link #parameters} list on each call. In other words, the map returned is built on fly and is a copy.
     * Any change to this map is NOT reflected on list and other way around!
     */
    public Map<String, Parameter> getParameterMap() {
        LinkedHashMap<String, Parameter> parameterMap = new LinkedHashMap<>();

        for (Parameter pd : parameters) {
            parameterMap.put(pd.getName(), pd);
        }

        return parameterMap;
    }

    // ----------------------------------------------------------------------
    // Dependency requirement
    // ----------------------------------------------------------------------

    /**
     * @param requiresDependencyResolution the new required dependencies in a specified scope
     */
    public void setDependencyResolutionRequired(String requiresDependencyResolution) {
        this.dependencyResolutionRequired = requiresDependencyResolution;
    }

    public String getDependencyResolutionRequired() {
        return dependencyResolutionRequired;
    }

    /**
     * @return the required dependencies in a specified scope
     * TODO the name is not intelligible
     */
    @Deprecated
    public String isDependencyResolutionRequired() {
        return dependencyResolutionRequired;
    }

    /**
     * @since 3.0-alpha-3
     */
    public void setDependencyCollectionRequired(String requiresDependencyCollection) {
        this.dependencyCollectionRequired = requiresDependencyCollection;
    }

    /**
     * Gets the scope of (transitive) dependencies that should be collected. Dependency collection refers to the process
     * of calculating the complete dependency tree in terms of artifact coordinates. In contrast to dependency
     * resolution, this does not include the download of the files for the dependency artifacts. It is meant for mojos
     * that only want to analyze the set of transitive dependencies, in particular during early lifecycle phases where
     * full dependency resolution might fail due to projects which haven't been built yet.
     *
     * @return The scope of (transitive) dependencies that should be collected or {@code null} if none.
     * @since 3.0-alpha-3
     */
    public String getDependencyCollectionRequired() {
        return dependencyCollectionRequired;
    }

    // ----------------------------------------------------------------------
    // Project requirement
    // ----------------------------------------------------------------------

    /**
     * @param requiresProject <code>true</code> if the Mojo needs a Maven project to be executed, <code>false</code>
     * otherwise.
     */
    public void setProjectRequired(boolean requiresProject) {
        this.projectRequired = requiresProject;
    }

    /**
     * @return <code>true</code> if the Mojo needs a Maven project to be executed, <code>false</code> otherwise.
     */
    public boolean isProjectRequired() {
        return projectRequired;
    }

    // ----------------------------------------------------------------------
    // Online vs. Offline requirement
    // ----------------------------------------------------------------------

    /**
     * @param requiresOnline <code>true</code> if the Mojo is online, <code>false</code> otherwise.
     */
    public void setOnlineRequired(boolean requiresOnline) {
        this.onlineRequired = requiresOnline;
    }

    /**
     * @return <code>true</code> if the Mojo is online, <code>false</code> otherwise.
     */
    // blech! this isn't even intelligible as a method name. provided for
    // consistency...
    public boolean isOnlineRequired() {
        return onlineRequired;
    }

    /**
     * @return <code>true</code> if the Mojo is online, <code>false</code> otherwise.
     */
    // more english-friendly method...keep the code clean! :)
    public boolean requiresOnline() {
        return onlineRequired;
    }

    /**
     * @return the bound phase name of the Mojo
     */
    public String getPhase() {
        return phase;
    }

    /**
     * @param phase the new bound phase name of the Mojo
     */
    public void setPhase(String phase) {
        this.phase = phase;
    }

    /**
     * @return the version when the Mojo was added to the API
     */
    public String getSince() {
        return since;
    }

    /**
     * @param since the new version when the Mojo was added to the API
     */
    public void setSince(String since) {
        this.since = since;
    }

    /**
     * @return The goal name of the Mojo
     */
    public String getGoal() {
        return goal;
    }

    /**
     * @param goal The new goal name of the Mojo
     */
    public void setGoal(String goal) {
        this.goal = goal;
    }

    /**
     * @return the invocation phase of the Mojo
     */
    public String getExecutePhase() {
        return executePhase;
    }

    /**
     * @param executePhase the new invocation phase of the Mojo
     */
    public void setExecutePhase(String executePhase) {
        this.executePhase = executePhase;
    }

    /**
     * @return <code>true</code> if the Mojo uses <code>always</code> for the <code>executionStrategy</code>
     */
    public boolean alwaysExecute() {
        return MULTI_PASS_EXEC_STRATEGY.equals(executionStrategy);
    }

    /**
     * @return the execution strategy
     */
    public String getExecutionStrategy() {
        return executionStrategy;
    }

    /**
     * @param executionStrategy the new execution strategy
     */
    public void setExecutionStrategy(String executionStrategy) {
        this.executionStrategy = executionStrategy;
    }

    /**
     * @return the mojo configuration
     */
    public PlexusConfiguration getMojoConfiguration() {
        if (mojoConfiguration == null) {
            mojoConfiguration = new XmlPlexusConfiguration("configuration");
        }
        return mojoConfiguration;
    }

    /**
     * @param mojoConfiguration a new mojo configuration
     */
    public void setMojoConfiguration(PlexusConfiguration mojoConfiguration) {
        this.mojoConfiguration = mojoConfiguration;
    }

    /** {@inheritDoc} */
    public String getRole() {
        return Mojo.ROLE;
    }

    /** {@inheritDoc} */
    public String getRoleHint() {
        return getId();
    }

    /**
     * @return the id of the mojo, based on the goal name
     */
    public String getId() {
        return getPluginDescriptor().getId() + ":" + getGoal();
    }

    /**
     * @return the full goal name
     * @see PluginDescriptor#getGoalPrefix()
     * @see #getGoal()
     */
    public String getFullGoalName() {
        return getPluginDescriptor().getGoalPrefix() + ":" + getGoal();
    }

    /** {@inheritDoc} */
    public String getComponentType() {
        return MAVEN_PLUGIN;
    }

    /**
     * @return the plugin descriptor
     */
    public PluginDescriptor getPluginDescriptor() {
        return pluginDescriptor;
    }

    /**
     * @param pluginDescriptor the new plugin descriptor
     */
    public void setPluginDescriptor(PluginDescriptor pluginDescriptor) {
        this.pluginDescriptor = pluginDescriptor;
    }

    /**
     * @return <code>true</code> if the Mojo is inherited, <code>false</code> otherwise.
     */
    public boolean isInheritedByDefault() {
        return inheritedByDefault;
    }

    /**
     * @param inheritedByDefault <code>true</code> if the Mojo is inherited, <code>false</code> otherwise.
     */
    public void setInheritedByDefault(boolean inheritedByDefault) {
        this.inheritedByDefault = inheritedByDefault;
    }

    /** {@inheritDoc} */
    public boolean equals(Object object) {
        if (this == object) {
            return true;
        }

        if (object instanceof MojoDescriptor) {
            MojoDescriptor other = (MojoDescriptor) object;

            return Objects.equals(getPluginDescriptor(), other.getPluginDescriptor())
                    && Objects.equals(getGoal(), other.getGoal());
        }

        return false;
    }

    /** {@inheritDoc} */
    public int hashCode() {
        return Objects.hash(getGoal(), getPluginDescriptor());
    }

    /**
     * @return the invocation lifecycle of the Mojo
     */
    public String getExecuteLifecycle() {
        return executeLifecycle;
    }

    /**
     * @param executeLifecycle the new invocation lifecycle of the Mojo
     */
    public void setExecuteLifecycle(String executeLifecycle) {
        this.executeLifecycle = executeLifecycle;
    }

    /**
     * @param aggregator <code>true</code> if the Mojo uses the Maven project and its child modules,
     * <code>false</code> otherwise.
     */
    public void setAggregator(boolean aggregator) {
        this.aggregator = aggregator;
    }

    /**
     * @return <code>true</code> if the Mojo uses the Maven project and its child modules,
     * <code>false</code> otherwise.
     */
    public boolean isAggregator() {
        return aggregator;
    }

    /**
     * @return <code>true</code> if the Mojo cannot be invoked directly, <code>false</code> otherwise.
     */
    public boolean isDirectInvocationOnly() {
        return directInvocationOnly;
    }

    /**
     * @param directInvocationOnly <code>true</code> if the Mojo cannot be invoked directly,
     * <code>false</code> otherwise.
     */
    public void setDirectInvocationOnly(boolean directInvocationOnly) {
        this.directInvocationOnly = directInvocationOnly;
    }

    /**
     * @return <code>true</code> if the Mojo needs reports to run, <code>false</code> otherwise.
     */
    public boolean isRequiresReports() {
        return requiresReports;
    }

    /**
     * @param requiresReports <code>true</code> if the Mojo needs reports to run, <code>false</code> otherwise.
     */
    public void setRequiresReports(boolean requiresReports) {
        this.requiresReports = requiresReports;
    }

    /**
     * @param executeGoal the new invocation goal of the Mojo
     */
    public void setExecuteGoal(String executeGoal) {
        this.executeGoal = executeGoal;
    }

    /**
     * @return the invocation goal of the Mojo
     */
    public String getExecuteGoal() {
        return executeGoal;
    }

    /**
     * @return True if the <code>Mojo</code> is thread-safe and can be run safely in parallel
     * @since 3.0-beta-2
     */
    public boolean isThreadSafe() {
        return threadSafe;
    }

    /**
     * @param threadSafe indicates that the mojo is thread-safe and can be run safely in parallel
     * @since 3.0-beta-2
     */
    public void setThreadSafe(boolean threadSafe) {
        this.threadSafe = threadSafe;
    }

    /**
     * @return {@code true} if this mojo forks either a goal or the lifecycle, {@code false} otherwise.
     */
    public boolean isForking() {
        return (getExecuteGoal() != null && getExecuteGoal().length() > 0)
                || (getExecutePhase() != null && getExecutePhase().length() > 0);
    }

    /**
     * Creates a shallow copy of this mojo descriptor.
     */
    @Override
    public MojoDescriptor clone() {
        try {
            return (MojoDescriptor) super.clone();
        } catch (CloneNotSupportedException e) {
            throw new UnsupportedOperationException(e);
        }
    }
}
