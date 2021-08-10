package org.apache.maven.caching.xml;

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

import com.google.common.base.Supplier;
import com.google.common.base.Suppliers;
import org.apache.commons.lang3.StringUtils;
import org.apache.maven.caching.DefaultPluginScanConfig;
import org.apache.maven.caching.PluginScanConfig;
import org.apache.maven.caching.PluginScanConfigImpl;
import org.apache.maven.caching.checksum.MultimoduleDiscoveryStrategy;
import org.apache.maven.caching.hash.HashFactory;
import org.apache.maven.caching.jaxb.CacheType;
import org.apache.maven.caching.jaxb.ConfigurationType;
import org.apache.maven.caching.jaxb.CoordinatesBaseType;
import org.apache.maven.caching.jaxb.ExecutablesType;
import org.apache.maven.caching.jaxb.ExecutionConfigurationScanType;
import org.apache.maven.caching.jaxb.ExecutionControlType;
import org.apache.maven.caching.jaxb.ExecutionIdsListType;
import org.apache.maven.caching.jaxb.GoalReconciliationType;
import org.apache.maven.caching.jaxb.GoalsListType;
import org.apache.maven.caching.jaxb.PathSetType;
import org.apache.maven.caching.jaxb.PluginConfigurationScanType;
import org.apache.maven.caching.jaxb.PluginSetType;
import org.apache.maven.caching.jaxb.PropertyNameType;
import org.apache.maven.caching.jaxb.TrackedPropertyType;
import org.apache.maven.execution.MavenSession;
import org.apache.maven.model.Plugin;
import org.apache.maven.model.PluginExecution;
import org.apache.maven.plugin.MojoExecution;
import org.apache.maven.project.MavenProject;
import org.codehaus.plexus.component.annotations.Component;
import org.codehaus.plexus.component.annotations.Requirement;
import org.codehaus.plexus.logging.Logger;

import javax.annotation.Nonnull;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.regex.Pattern;

import static com.google.common.base.Preconditions.checkState;
import static java.lang.Boolean.TRUE;
import static java.lang.Boolean.parseBoolean;
import static org.apache.maven.caching.ProjectUtils.getMultimoduleRoot;

/**
 * CacheConfigImpl
 */
@Component( role = CacheConfig.class,
            instantiationStrategy = "singleton" )
public class CacheConfigImpl implements CacheConfig
{

    public static final String CONFIG_PATH_PROPERTY_NAME = "remote.cache.configPath";
    public static final String CACHE_ENABLED_PROPERTY_NAME = "remote.cache.enabled";
    public static final String SAVE_TO_REMOTE_PROPERTY_NAME = "remote.cache.save.enabled";
    public static final String SAVE_NON_OVERRIDEABLE_NAME = "remote.cache.save.final";
    public static final String FAIL_FAST_PROPERTY_NAME = "remote.cache.failFast";
    public static final String BASELINE_BUILD_URL_PROPERTY_NAME = "remote.cache.baselineUrl";

    @Requirement
    private Logger logger;

    @Requirement
    private XmlService xmlService;

    private volatile CacheState state = CacheState.NOT_INITIALIZED;
    private volatile CacheType cacheConfig;
    private volatile HashFactory hashFactory;

    private final Supplier<List<Pattern>> excludePatterns = Suppliers.memoize( new Supplier<List<Pattern>>()
    {
        @Override
        public List<Pattern> get()
        {
            return compileExcludePatterns();
        }
    } );


    @Override
    public synchronized CacheState initialize( MavenProject project, MavenSession session )
    {

        if ( state != CacheState.NOT_INITIALIZED )
        {
            return state;
        }

        final String enabled = System.getProperty( CACHE_ENABLED_PROPERTY_NAME, "true" );
        if ( !parseBoolean( enabled ) )
        {
            logger.info( "Cache disabled by command line flag, project will be built fully and not cached" );
            state = CacheState.DISABLED;
            return state;
        }

        Path configPath = null;

        String configPathText = System.getProperty( CONFIG_PATH_PROPERTY_NAME );
        if ( StringUtils.isNotBlank( configPathText ) )
        {
            configPath = Paths.get( configPathText );
        }
        if ( configPath == null )
        {
            configPathText = project.getProperties().getProperty( CONFIG_PATH_PROPERTY_NAME );
            if ( StringUtils.isNotBlank( configPathText ) )
            {
                configPath = Paths.get( configPathText );
            }
        }

        if ( configPath == null )
        {
            configPath = Paths.get( getMultimoduleRoot( session ), ".mvn", "maven-cache-config.xml" );
        }

        if ( !Files.exists( configPath ) )
        {
            logger.warn(
                    "Cache configuration is not available at configured path " + configPath + ", cache is disabled" );
            state = CacheState.DISABLED;
            return state;
        }

        try
        {
            logger.info( "Loading cache configuration from " + configPath );
            cacheConfig = xmlService.fromFile( CacheType.class, configPath.toFile() );
        }
        catch ( Exception e )
        {
            throw new IllegalArgumentException(
                    "Cannot initialize cache because xml config is not valid or not available", e );
        }

        if ( !cacheConfig.getConfiguration().isEnabled() )
        {
            state = CacheState.DISABLED;
            return state;
        }

        String hashAlgorithm = null;
        try
        {
            hashAlgorithm = getConfiguration().getHashAlgorithm();
            hashFactory = HashFactory.of( hashAlgorithm );
            logger.info( "Using " + hashAlgorithm + " hash algorithm for cache" );
        }
        catch ( Exception e )
        {
            throw new IllegalArgumentException( "Unsupported hashing algorithm: " + hashAlgorithm, e );
        }

        state = CacheState.INITIALIZED;
        return state;
    }

    @Nonnull
    @Override
    public List<TrackedPropertyType> getTrackedProperties( MojoExecution mojoExecution )
    {
        checkInitializedState();
        final GoalReconciliationType reconciliationConfig = findReconciliationConfig( mojoExecution );
        if ( reconciliationConfig != null )
        {
            return reconciliationConfig.getReconcile();
        }
        else
        {
            return Collections.emptyList();
        }
    }

    @Override
    public boolean isLogAllProperties( MojoExecution mojoExecution )
    {
        final GoalReconciliationType reconciliationConfig = findReconciliationConfig( mojoExecution );
        if ( reconciliationConfig != null && reconciliationConfig.isLogAll() )
        {
            return true;
        }
        return cacheConfig.isSetExecutionControl() && cacheConfig.getExecutionControl().isSetReconcile()
                && cacheConfig.getExecutionControl().getReconcile().isLogAllProperties();
    }

    private GoalReconciliationType findReconciliationConfig( MojoExecution mojoExecution )
    {

        if ( !cacheConfig.isSetExecutionControl() )
        {
            return null;
        }

        final ExecutionControlType executionControl = cacheConfig.getExecutionControl();
        if ( !executionControl.isSetReconcile() )
        {
            return null;
        }

        final List<GoalReconciliationType> reconciliation = executionControl.getReconcile().getPlugin();

        for ( GoalReconciliationType goalReconciliationConfig : reconciliation )
        {

            final String goal = mojoExecution.getGoal();

            if ( isPluginMatch( mojoExecution.getPlugin(), goalReconciliationConfig ) && StringUtils.equals( goal,
                    goalReconciliationConfig.getGoal() ) )
            {
                return goalReconciliationConfig;
            }
        }
        return null;
    }

    @Nonnull
    @Override
    public List<PropertyNameType> getLoggedProperties( MojoExecution mojoExecution )
    {
        checkInitializedState();

        final GoalReconciliationType reconciliationConfig = findReconciliationConfig( mojoExecution );
        if ( reconciliationConfig != null )
        {
            return reconciliationConfig.getLog();
        }
        else
        {
            return Collections.emptyList();
        }
    }

    @Nonnull
    @Override
    public List<PropertyNameType> getNologProperties( MojoExecution mojoExecution )
    {
        checkInitializedState();
        final GoalReconciliationType reconciliationConfig = findReconciliationConfig( mojoExecution );
        if ( reconciliationConfig != null )
        {
            return reconciliationConfig.getNolog();
        }
        else
        {
            return Collections.emptyList();
        }
    }

    @Nonnull
    @Override
    public List<String> getEffectivePomExcludeProperties( Plugin plugin )
    {
        checkInitializedState();
        final PluginConfigurationScanType pluginConfig = findPluginScanConfig( plugin );

        if ( pluginConfig != null && pluginConfig.isSetEffectivePom() )
        {
            return pluginConfig.getEffectivePom().getExcludeProperty();
        }
        return Collections.emptyList();
    }

    private PluginConfigurationScanType findPluginScanConfig( Plugin plugin )
    {

        if ( !cacheConfig.isSetInput() )
        {
            return null;
        }

        final List<PluginConfigurationScanType> pluginConfigs = cacheConfig.getInput().getPlugin();
        for ( PluginConfigurationScanType pluginConfig : pluginConfigs )
        {
            if ( isPluginMatch( plugin, pluginConfig ) )
            {
                return pluginConfig;
            }
        }
        return null;
    }

    private boolean isPluginMatch( Plugin plugin, CoordinatesBaseType pluginConfig )
    {
        return StringUtils.equals( pluginConfig.getArtifactId(),
                plugin.getArtifactId() ) && ( !pluginConfig.isSetGroupId() || StringUtils.equals(
                pluginConfig.getGroupId(), plugin.getGroupId() ) );
    }


    @Nonnull
    @Override
    public PluginScanConfig getPluginDirScanConfig( Plugin plugin )
    {
        checkInitializedState();
        final PluginConfigurationScanType pluginConfig = findPluginScanConfig( plugin );
        if ( pluginConfig == null || !pluginConfig.isSetDirScan() )
        {
            return new DefaultPluginScanConfig();
        }

        return new PluginScanConfigImpl( pluginConfig.getDirScan() );
    }

    @Nonnull
    @Override
    public PluginScanConfig getExecutionDirScanConfig( Plugin plugin, PluginExecution exec )
    {
        checkInitializedState();
        final PluginConfigurationScanType pluginScanConfig = findPluginScanConfig( plugin );

        if ( pluginScanConfig != null )
        {
            final ExecutionConfigurationScanType executionScanConfig = findExecutionScanConfig( exec,
                    pluginScanConfig.getExecution() );
            if ( executionScanConfig != null && executionScanConfig.isSetDirScan() )
            {
                return new PluginScanConfigImpl( executionScanConfig.getDirScan() );
            }
        }

        return new DefaultPluginScanConfig();
    }

    private ExecutionConfigurationScanType findExecutionScanConfig( PluginExecution execution,
                                                                    List<ExecutionConfigurationScanType> scanConfigs )
    {
        for ( ExecutionConfigurationScanType executionScanConfig : scanConfigs )
        {
            if ( executionScanConfig.getExecId().contains( execution.getId() ) )
            {
                return executionScanConfig;
            }
        }
        return null;
    }

    @Override
    public String isProcessPlugins()
    {
        checkInitializedState();
        return TRUE.toString();
    }

    @Override
    public String getDefaultGlob()
    {
        checkInitializedState();
        return StringUtils.trim( cacheConfig.getInput().getGlobal().getGlob() );
    }

    @Nonnull
    @Override
    public List<PathSetType.Include> getGlobalIncludePaths()
    {
        checkInitializedState();
        return cacheConfig.getInput().getGlobal().getInclude();
    }

    @Nonnull
    @Override
    public List<String> getGlobalExcludePaths()
    {
        checkInitializedState();
        return cacheConfig.getInput().getGlobal().getExclude();
    }

    @Nonnull
    @Override
    public MultimoduleDiscoveryStrategy getMultimoduleDiscoveryStrategy()
    {
        checkInitializedState();
        final ConfigurationType.ProjectDiscoveryStrategy projectDiscoveryStrategy =
                cacheConfig.getConfiguration().getProjectDiscoveryStrategy();
        if ( projectDiscoveryStrategy.isSetSpecificVersion() )
        {
            return new SentinelVersionStartegy( projectDiscoveryStrategy.getSpecificVersion() );
        }
        return new AllExternalSrategy();
    }

    @Nonnull
    @Override
    public HashFactory getHashFactory()
    {
        checkInitializedState();
        return hashFactory;
    }

    @Override
    public boolean canIgnore( MojoExecution mojoExecution )
    {
        checkInitializedState();
        if ( !cacheConfig.isSetExecutionControl() || !cacheConfig.getExecutionControl().isSetIgnoreMissing() )
        {
            return false;
        }

        return executionMatches( mojoExecution, cacheConfig.getExecutionControl().getIgnoreMissing() );
    }

    @Override
    public boolean isForcedExecution( MojoExecution execution )
    {
        checkInitializedState();
        if ( !cacheConfig.isSetExecutionControl() || !cacheConfig.getExecutionControl().isSetRunAlways() )
        {
            return false;
        }

        return executionMatches( execution, cacheConfig.getExecutionControl().getRunAlways() );
    }

    private boolean executionMatches( MojoExecution execution, ExecutablesType executablesType )
    {
        final List<PluginSetType> pluginConfigs = executablesType.getPlugin();
        for ( PluginSetType pluginConfig : pluginConfigs )
        {
            if ( isPluginMatch( execution.getPlugin(), pluginConfig ) )
            {
                return true;
            }
        }

        final List<ExecutionIdsListType> executionIds = executablesType.getExecution();
        for ( ExecutionIdsListType executionConfig : executionIds )
        {
            if ( isPluginMatch( execution.getPlugin(), executionConfig ) && executionConfig.getExecId().contains(
                    execution.getExecutionId() ) )
            {
                return true;
            }
        }

        final List<GoalsListType> pluginsGoalsList = executablesType.getGoals();
        for ( GoalsListType pluginGoals : pluginsGoalsList )
        {
            if ( isPluginMatch( execution.getPlugin(), pluginGoals ) && pluginGoals.getGoal().contains(
                    execution.getGoal() ) )
            {
                return true;
            }
        }

        return false;
    }

    @Override
    public boolean isEnabled()
    {
        return state == CacheState.INITIALIZED;
    }


    @Override
    public boolean isRemoteCacheEnabled()
    {
        checkInitializedState();
        return getRemote().isEnabled();
    }

    @Override
    public boolean isSaveToRemote()
    {
        checkInitializedState();
        return Boolean.getBoolean( SAVE_TO_REMOTE_PROPERTY_NAME ) || getRemote().isSaveToRemote();
    }

    @Override
    public boolean isSaveFinal()
    {
        return Boolean.getBoolean( SAVE_NON_OVERRIDEABLE_NAME );
    }

    @Override
    public boolean isFailFast()
    {
        return Boolean.getBoolean( FAIL_FAST_PROPERTY_NAME );
    }

    @Override
    public boolean isBaselineDiffEnabled()
    {
        return System.getProperties().containsKey( BASELINE_BUILD_URL_PROPERTY_NAME );
    }

    @Override
    public String getBaselineCacheUrl()
    {
        return System.getProperty( BASELINE_BUILD_URL_PROPERTY_NAME );
    }

    @Override
    public String getUrl()
    {
        checkInitializedState();
        return getRemote().getUrl();
    }


    @Override
    public int getMaxLocalBuildsCached()
    {
        checkInitializedState();
        return getLocal().getMaxBuildsCached().intValue();
    }

    @Override
    public List<String> getAttachedOutputs()
    {
        checkInitializedState();
        final ConfigurationType.AttachedOutputs attachedOutputs = getConfiguration().getAttachedOutputs();
        return attachedOutputs == null ? Collections.<String>emptyList() : attachedOutputs.getDirName();
    }

    @Nonnull
    @Override
    public List<Pattern> getExcludePatterns()
    {
        checkInitializedState();
        return excludePatterns.get();
    }

    private List<Pattern> compileExcludePatterns()
    {
        if ( cacheConfig.isSetOutput() && cacheConfig.getOutput().isSetExclude() )
        {
            List<Pattern> patterns = new ArrayList<>();
            for ( String pattern : cacheConfig.getOutput().getExclude().getPattern() )
            {
                patterns.add( Pattern.compile( pattern ) );
            }
            return patterns;
        }
        return Collections.emptyList();
    }

    private ConfigurationType.Remote getRemote()
    {
        return getConfiguration().getRemote();
    }

    private ConfigurationType.Local getLocal()
    {
        return getConfiguration().getLocal();
    }

    private ConfigurationType getConfiguration()
    {
        return cacheConfig.getConfiguration();
    }

    private void checkInitializedState()
    {
        checkState( state == CacheState.INITIALIZED, "Cache is not initialized. Actual state: " + state );
    }
}
