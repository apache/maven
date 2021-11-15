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

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.regex.Pattern;

import javax.annotation.Nonnull;

import org.apache.commons.lang3.StringUtils;
import org.apache.maven.caching.DefaultPluginScanConfig;
import org.apache.maven.caching.PluginScanConfig;
import org.apache.maven.caching.PluginScanConfigImpl;
import org.apache.maven.caching.checksum.MultimoduleDiscoveryStrategy;
import org.apache.maven.caching.hash.HashFactory;
import org.apache.maven.caching.xml.config.AttachedOutputs;
import org.apache.maven.caching.xml.config.CacheConfig;
import org.apache.maven.caching.xml.config.Configuration;
import org.apache.maven.caching.xml.config.CoordinatesBase;
import org.apache.maven.caching.xml.config.Exclude;
import org.apache.maven.caching.xml.config.Executables;
import org.apache.maven.caching.xml.config.ExecutionConfigurationScan;
import org.apache.maven.caching.xml.config.ExecutionControl;
import org.apache.maven.caching.xml.config.ExecutionIdsList;
import org.apache.maven.caching.xml.config.GoalReconciliation;
import org.apache.maven.caching.xml.config.GoalsList;
import org.apache.maven.caching.xml.config.Include;
import org.apache.maven.caching.xml.config.Local;
import org.apache.maven.caching.xml.config.PluginConfigurationScan;
import org.apache.maven.caching.xml.config.PluginSet;
import org.apache.maven.caching.xml.config.ProjectDiscoveryStrategy;
import org.apache.maven.caching.xml.config.PropertyName;
import org.apache.maven.caching.xml.config.Remote;
import org.apache.maven.caching.xml.config.TrackedProperty;
import org.apache.maven.execution.MavenSession;
import org.apache.maven.feature.Features;
import org.apache.maven.model.Plugin;
import org.apache.maven.model.PluginExecution;
import org.apache.maven.plugin.MojoExecution;
import org.codehaus.plexus.logging.Logger;

import static java.lang.Boolean.TRUE;
import static java.lang.Boolean.parseBoolean;
import static org.apache.maven.caching.ProjectUtils.getMultimoduleRoot;

/**
 * CacheConfigImpl
 */
public class CacheConfigImpl implements org.apache.maven.caching.xml.CacheConfig
{

    public static final String CONFIG_PATH_PROPERTY_NAME = "remote.cache.configPath";
    public static final String SAVE_TO_REMOTE_PROPERTY_NAME = "remote.cache.save.enabled";
    public static final String SAVE_NON_OVERRIDEABLE_NAME = "remote.cache.save.final";
    public static final String FAIL_FAST_PROPERTY_NAME = "remote.cache.failFast";
    public static final String BASELINE_BUILD_URL_PROPERTY_NAME = "remote.cache.baselineUrl";

    private final MavenSession session;
    private final CacheState state;
    private final CacheConfig cacheConfig;
    private final HashFactory hashFactory;
    private final List<Pattern> excludePatterns;

    public CacheConfigImpl( Logger logger, XmlService xmlService, MavenSession session )
    {
        this.session = session;

        if ( !Features.caching( session.getUserProperties() ).isActive() )
        {
            logger.info( "Cache disabled by command line flag, project will be built fully and not cached" );
            state = CacheState.DISABLED;
            cacheConfig = null;
            hashFactory = null;
            excludePatterns = null;
        }
        else
        {
            Path configPath;

            String configPathText = getProperty( CONFIG_PATH_PROPERTY_NAME, null );
            if ( StringUtils.isNotBlank( configPathText ) )
            {
                configPath = Paths.get( configPathText );
            }
            else
            {
                configPath = getMultimoduleRoot( session ).resolve( ".mvn" ).resolve( "maven-cache-config.xml" );
            }

            if ( !Files.exists( configPath ) )
            {
                logger.warn( "Cache configuration is not available at configured path "
                                + configPath + ", cache is disabled" );
                state = CacheState.DISABLED;
                cacheConfig = null;
                hashFactory = null;
                excludePatterns = null;
            }
            else
            {
                try
                {
                    logger.info( "Loading cache configuration from " + configPath );
                    cacheConfig = xmlService.loadCacheConfig( configPath.toFile() );
                }
                catch ( Exception e )
                {
                    throw new IllegalArgumentException(
                            "Cannot initialize cache because xml config is not valid or not available", e );
                }

                if ( !cacheConfig.getConfiguration().isEnabled() )
                {
                    state = CacheState.DISABLED;
                    hashFactory = null;
                    excludePatterns = null;
                }
                else
                {
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

                    excludePatterns = compileExcludePatterns();
                    state = CacheState.INITIALIZED;
                }
            }
        }
    }

    @Nonnull
    @Override
    public CacheState getState()
    {
        return state;
    }

    @Nonnull
    @Override
    public List<TrackedProperty> getTrackedProperties( MojoExecution mojoExecution )
    {
        checkInitializedState();
        final GoalReconciliation reconciliationConfig = findReconciliationConfig( mojoExecution );
        if ( reconciliationConfig != null )
        {
            return reconciliationConfig.getReconciles();
        }
        else
        {
            return Collections.emptyList();
        }
    }

    @Override
    public boolean isLogAllProperties( MojoExecution mojoExecution )
    {
        final GoalReconciliation reconciliationConfig = findReconciliationConfig( mojoExecution );
        if ( reconciliationConfig != null && reconciliationConfig.isLogAll() )
        {
            return true;
        }
        return cacheConfig.getExecutionControl() != null && cacheConfig.getExecutionControl().getReconcile() != null
                && cacheConfig.getExecutionControl().getReconcile().isLogAllProperties();
    }

    private GoalReconciliation findReconciliationConfig( MojoExecution mojoExecution )
    {

        if ( cacheConfig.getExecutionControl() == null )
        {
            return null;
        }

        final ExecutionControl executionControl = cacheConfig.getExecutionControl();
        if ( executionControl.getReconcile() == null )
        {
            return null;
        }

        final List<GoalReconciliation> reconciliation = executionControl.getReconcile().getPlugins();

        for ( GoalReconciliation goalReconciliationConfig : reconciliation )
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
    public List<PropertyName> getLoggedProperties( MojoExecution mojoExecution )
    {
        checkInitializedState();

        final GoalReconciliation reconciliationConfig = findReconciliationConfig( mojoExecution );
        if ( reconciliationConfig != null )
        {
            return reconciliationConfig.getLogs();
        }
        else
        {
            return Collections.emptyList();
        }
    }

    @Nonnull
    @Override
    public List<PropertyName> getNologProperties( MojoExecution mojoExecution )
    {
        checkInitializedState();
        final GoalReconciliation reconciliationConfig = findReconciliationConfig( mojoExecution );
        if ( reconciliationConfig != null )
        {
            return reconciliationConfig.getNologs();
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
        final PluginConfigurationScan pluginConfig = findPluginScanConfig( plugin );

        if ( pluginConfig != null && pluginConfig.getEffectivePom() != null )
        {
            return pluginConfig.getEffectivePom().getExcludeProperties();
        }
        return Collections.emptyList();
    }

    private PluginConfigurationScan findPluginScanConfig( Plugin plugin )
    {

        if ( cacheConfig.getInput() == null )
        {
            return null;
        }

        final List<PluginConfigurationScan> pluginConfigs = cacheConfig.getInput().getPlugins();
        for ( PluginConfigurationScan pluginConfig : pluginConfigs )
        {
            if ( isPluginMatch( plugin, pluginConfig ) )
            {
                return pluginConfig;
            }
        }
        return null;
    }

    private boolean isPluginMatch( Plugin plugin, CoordinatesBase pluginConfig )
    {
        return StringUtils.equals( pluginConfig.getArtifactId(),
                plugin.getArtifactId() ) && ( pluginConfig.getGroupId() == null || StringUtils.equals(
                pluginConfig.getGroupId(), plugin.getGroupId() ) );
    }


    @Nonnull
    @Override
    public PluginScanConfig getPluginDirScanConfig( Plugin plugin )
    {
        checkInitializedState();
        final PluginConfigurationScan pluginConfig = findPluginScanConfig( plugin );
        if ( pluginConfig == null || pluginConfig.getDirScan() == null )
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
        final PluginConfigurationScan pluginScanConfig = findPluginScanConfig( plugin );

        if ( pluginScanConfig != null )
        {
            final ExecutionConfigurationScan executionScanConfig = findExecutionScanConfig( exec,
                    pluginScanConfig.getExecutions() );
            if ( executionScanConfig != null && executionScanConfig.getDirScan() != null )
            {
                return new PluginScanConfigImpl( executionScanConfig.getDirScan() );
            }
        }

        return new DefaultPluginScanConfig();
    }

    private ExecutionConfigurationScan findExecutionScanConfig( PluginExecution execution,
                                                                    List<ExecutionConfigurationScan> scanConfigs )
    {
        for ( ExecutionConfigurationScan executionScanConfig : scanConfigs )
        {
            if ( executionScanConfig.getExecIds().contains( execution.getId() ) )
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
    public List<Include> getGlobalIncludePaths()
    {
        checkInitializedState();
        return cacheConfig.getInput().getGlobal().getIncludes();
    }

    @Nonnull
    @Override
    public List<Exclude> getGlobalExcludePaths()
    {
        checkInitializedState();
        return cacheConfig.getInput().getGlobal().getExcludes();
    }

    @Nonnull
    @Override
    public MultimoduleDiscoveryStrategy getMultimoduleDiscoveryStrategy()
    {
        checkInitializedState();
        final ProjectDiscoveryStrategy projectDiscoveryStrategy =
                cacheConfig.getConfiguration().getProjectDiscoveryStrategy();
        if ( projectDiscoveryStrategy != null && projectDiscoveryStrategy.getSpecificVersion() != null )
        {
            return new SentinelVersionStrategy( projectDiscoveryStrategy.getSpecificVersion() );
        }
        return new AllExternalStrategy();
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
        if ( cacheConfig.getExecutionControl() == null || cacheConfig.getExecutionControl().getIgnoreMissing() == null )
        {
            return false;
        }

        return executionMatches( mojoExecution, cacheConfig.getExecutionControl().getIgnoreMissing() );
    }

    @Override
    public boolean isForcedExecution( MojoExecution execution )
    {
        checkInitializedState();
        if ( cacheConfig.getExecutionControl() == null || cacheConfig.getExecutionControl().getRunAlways() == null )
        {
            return false;
        }

        return executionMatches( execution, cacheConfig.getExecutionControl().getRunAlways() );
    }

    private boolean executionMatches( MojoExecution execution, Executables executablesType )
    {
        final List<PluginSet> pluginConfigs = executablesType.getPlugins();
        for ( PluginSet pluginConfig : pluginConfigs )
        {
            if ( isPluginMatch( execution.getPlugin(), pluginConfig ) )
            {
                return true;
            }
        }

        final List<ExecutionIdsList> executionIds = executablesType.getExecutions();
        for ( ExecutionIdsList executionConfig : executionIds )
        {
            if ( isPluginMatch( execution.getPlugin(), executionConfig ) && executionConfig.getExecIds().contains(
                    execution.getExecutionId() ) )
            {
                return true;
            }
        }

        final List<GoalsList> pluginsGoalsList = executablesType.getGoalsLists();
        for ( GoalsList pluginGoals : pluginsGoalsList )
        {
            if ( isPluginMatch( execution.getPlugin(), pluginGoals ) && pluginGoals.getGoals().contains(
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
        return getProperty( BASELINE_BUILD_URL_PROPERTY_NAME, null ) != null;
    }

    @Override
    public String getBaselineCacheUrl()
    {
        return getProperty( BASELINE_BUILD_URL_PROPERTY_NAME, null );
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
        return getLocal().getMaxBuildsCached();
    }

    @Override
    public List<String> getAttachedOutputs()
    {
        checkInitializedState();
        final AttachedOutputs attachedOutputs = getConfiguration().getAttachedOutputs();
        return attachedOutputs == null ? Collections.<String>emptyList() : attachedOutputs.getDirNames();
    }

    @Nonnull
    @Override
    public List<Pattern> getExcludePatterns()
    {
        checkInitializedState();
        return excludePatterns;
    }

    private List<Pattern> compileExcludePatterns()
    {
        if ( cacheConfig.getOutput() != null && cacheConfig.getOutput().getExclude() != null )
        {
            List<Pattern> patterns = new ArrayList<>();
            for ( String pattern : cacheConfig.getOutput().getExclude().getPatterns() )
            {
                patterns.add( Pattern.compile( pattern ) );
            }
            return patterns;
        }
        return Collections.emptyList();
    }

    private Remote getRemote()
    {
        return getConfiguration().getRemote();
    }

    private Local getLocal()
    {
        return getConfiguration().getLocal();
    }

    private Configuration getConfiguration()
    {
        return cacheConfig.getConfiguration();
    }

    private void checkInitializedState()
    {
        if ( state != CacheState.INITIALIZED )
        {
            throw new IllegalStateException( "Cache is not initialized. Actual state: " + state );
        }
    }

    private String getProperty( String key, String defaultValue )
    {
        String value = session.getUserProperties().getProperty( key );
        if ( value == null )
        {
            value = session.getSystemProperties().getProperty( key );
            if ( value == null )
            {
                value = defaultValue;
            }
        }
        return value;
    }
}
