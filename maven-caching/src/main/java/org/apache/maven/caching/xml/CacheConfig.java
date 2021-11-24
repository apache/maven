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

import org.apache.maven.caching.PluginScanConfig;
import org.apache.maven.caching.hash.HashFactory;
import org.apache.maven.caching.xml.config.Exclude;
import org.apache.maven.caching.xml.config.Include;
import org.apache.maven.caching.xml.config.PropertyName;
import org.apache.maven.caching.xml.config.TrackedProperty;
import org.apache.maven.model.Plugin;
import org.apache.maven.model.PluginExecution;
import org.apache.maven.plugin.MojoExecution;

import javax.annotation.Nonnull;
import java.util.List;
import java.util.regex.Pattern;

/**
 * CacheConfig
 */
public interface CacheConfig
{

    @Nonnull
    CacheState initialize();

    @Nonnull
    List<TrackedProperty> getTrackedProperties( MojoExecution mojoExecution );

    boolean isLogAllProperties( MojoExecution mojoExecution );

    @Nonnull
    List<PropertyName> getLoggedProperties( MojoExecution mojoExecution );

    @Nonnull
    List<PropertyName> getNologProperties( MojoExecution mojoExecution );

    @Nonnull
    List<String> getEffectivePomExcludeProperties( Plugin plugin );

    String isProcessPlugins();

    String getDefaultGlob();

    @Nonnull
    List<Include> getGlobalIncludePaths();

    @Nonnull
    List<Exclude> getGlobalExcludePaths();

    @Nonnull
    PluginScanConfig getPluginDirScanConfig( Plugin plugin );

    @Nonnull
    PluginScanConfig getExecutionDirScanConfig( Plugin plugin, PluginExecution exec );

    @Nonnull
    HashFactory getHashFactory();

    boolean isForcedExecution( MojoExecution execution );

    String getUrl();

    boolean isEnabled();

    boolean isRemoteCacheEnabled();

    boolean isSaveToRemote();

    boolean isSaveFinal();

    boolean isFailFast();

    int getMaxLocalBuildsCached();

    List<String> getAttachedOutputs();

    boolean canIgnore( MojoExecution mojoExecution );

    @Nonnull
    List<Pattern> getExcludePatterns();

    boolean isBaselineDiffEnabled();

    String getBaselineCacheUrl();

    /**
     * Artifacts restore policy. Eager policy (default) resolves all cached artifacts before restoring project and
     * allows safe to fallback ro normal execution in case of restore failure. Lazy policy restores artifacts on demand
     * minimizing need for downloading any artifacts from cache
     * <p>
     * Use: -Dremote.cache.lazyRestore=(true|false)
     */
    boolean isLazyRestore();

    /**
     * Flag to restore (default) or not generated sources as it might be desired to disable it in continuous integration
     * scenarios
     */
    boolean isRestoreGeneratedSources();
}
